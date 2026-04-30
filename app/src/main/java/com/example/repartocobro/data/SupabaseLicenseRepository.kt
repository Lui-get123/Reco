package com.example.repartocobro.data

import android.content.ContentValues
import com.example.repartocobro.model.LicenseStatus
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Repositorio de licencias que valida códigos contra la tabla "Licencia" en Supabase.
 *
 * Flujo de validación:
 * 1. Consultar Supabase para verificar que el código exista.
 * 2. Verificar que el mes y año coincidan con la fecha actual.
 * 3. Verificar que el campo "usado" sea false.
 * 4. Si es válido: marcar como usado en Supabase + activar membresía local.
 * 5. Si no es válido: retornar error descriptivo.
 */
class SupabaseLicenseRepository(private val dbHelper: AppDatabaseHelper) {

    companion object {
        private const val SUPABASE_URL = "https://jlyecfmddblefnurinjx.supabase.co"
        private const val SUPABASE_ANON_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpseWVjZm1kZGJsZWZudXJpbmp4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzcyNjYzNzQsImV4cCI6MjA5Mjg0MjM3NH0.To_gCYERXuhlvd1itI0fnAQeHZ1g7zyJyv3DzNZ_sPM"
        private const val TABLE = "licencia"
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val monthNames = mapOf(
        Calendar.JANUARY to "ENERO",
        Calendar.FEBRUARY to "FEBRERO",
        Calendar.MARCH to "MARZO",
        Calendar.APRIL to "ABRIL",
        Calendar.MAY to "MAYO",
        Calendar.JUNE to "JUNIO",
        Calendar.JULY to "JULIO",
        Calendar.AUGUST to "AGOSTO",
        Calendar.SEPTEMBER to "SEPTIEMBRE",
        Calendar.OCTOBER to "OCTUBRE",
        Calendar.NOVEMBER to "NOVIEMBRE",
        Calendar.DECEMBER to "DICIEMBRE"
    )

    // ─────────────────────────────────────────────────────────────
    //  Resultado del canjeo
    // ─────────────────────────────────────────────────────────────

    sealed class RedeemResult {
        data class Success(val daysAdded: Int, val newExpiration: String) : RedeemResult()
        data class Error(val message: String) : RedeemResult()
    }

    // ─────────────────────────────────────────────────────────────
    //  Validar y canjear código vía Supabase
    // ─────────────────────────────────────────────────────────────

    /**
     * Valida un código contra Supabase (tabla Licencia) y si es válido,
     * lo marca como usado y activa la membresía local.
     *
     * DEBE ejecutarse en un hilo de fondo (coroutine / Dispatchers.IO).
     */
    fun redeemCode(code: String): RedeemResult {
        val trimmed = code.trim().uppercase()

        // 1. Validar formato básico: MESAÑO-TOKEN
        val parts = trimmed.split("-")
        if (parts.size != 2) {
            return RedeemResult.Error("❌ Formato inválido. Usa: MES+AÑO-CÓDIGO\n(Ej: ABRIL2026-AB12XZ)")
        }

        val prefixPart = parts[0]
        val tokenPart = parts[1]

        if (tokenPart.length !in 4..6 || !tokenPart.all { it.isLetterOrDigit() }) {
            return RedeemResult.Error("❌ El token debe tener 4–6 caracteres alfanuméricos.")
        }

        // 2. Extraer mes y año del código
        val (codeMes, codeAnio) = extractMonthYear(prefixPart)
            ?: return RedeemResult.Error("❌ No se pudo reconocer el mes/año del código.")

        // 3. Validar que coincidan con la fecha actual
        val cal = Calendar.getInstance()
        val currentMes = monthNames[cal.get(Calendar.MONTH)] ?: ""
        val currentAnio = cal.get(Calendar.YEAR)

        if (codeAnio != currentAnio) {
            return RedeemResult.Error(
                "❌ Este código es del año $codeAnio.\nSolo se pueden usar códigos del año actual ($currentAnio)."
            )
        }

        if (codeMes != currentMes) {
            val codeMonthIndex = monthNames.entries.firstOrNull { it.value == codeMes }?.key
            val currentMonthIndex = cal.get(Calendar.MONTH)
            return if (codeMonthIndex != null && codeMonthIndex > currentMonthIndex) {
                RedeemResult.Error("❌ Este código es para $codeMes $codeAnio.\nAún no puedes usarlo, espera al mes correspondiente.")
            } else {
                RedeemResult.Error("❌ Este código era para $codeMes $codeAnio.\nYa expiró, solicita el código del mes actual ($currentMes).")
            }
        }

        // 4. Consultar Supabase: buscar el código en la tabla Licencia
        try {
            val row = fetchLicenseFromSupabase(trimmed)
                ?: return RedeemResult.Error("❌ Código no encontrado.\nVerifica que el código sea correcto.")

            // 5. Verificar que el campo "usado" sea false
            val usado = row.optBoolean("usado", true)
            if (usado) {
                return RedeemResult.Error("❌ Este código ya fue utilizado.\nSolicita un código nuevo.")
            }

            // 6. Verificar mes y año en Supabase coincidan
            val supabaseMesNum = row.optInt("mes", 0)
            val supabaseAnio = row.optInt("año", 0)

            val currentMonthIndex = cal.get(Calendar.MONTH) + 1 // 1-12

            if (supabaseMesNum != currentMonthIndex || supabaseAnio != currentAnio) {
                return RedeemResult.Error(
                    "❌ El código no corresponde al mes actual.\nMes del código: $supabaseMesNum/$supabaseAnio"
                )
            }

            // 7. Marcar como usado en Supabase
            val marked = markCodeAsUsedInSupabase(trimmed)
            if (!marked) {
                return RedeemResult.Error("❌ Error al activar la licencia.\nIntenta de nuevo más tarde.")
            }

            // 8. Activar membresía local
            val newExpiration = activateLocalMembership()

            val daysRemaining = calculateDaysRemaining(newExpiration)

            return RedeemResult.Success(
                daysAdded = 30,
                newExpiration = newExpiration
            )

        } catch (e: java.net.UnknownHostException) {
            return RedeemResult.Error("❌ Sin conexión a internet.\nVerifica tu conexión e intenta de nuevo.")
        } catch (e: java.net.SocketTimeoutException) {
            return RedeemResult.Error("❌ Tiempo de espera agotado.\nVerifica tu conexión e intenta de nuevo.")
        } catch (e: Exception) {
            return RedeemResult.Error("❌ Error de conexión: ${e.localizedMessage ?: "desconocido"}\nIntenta de nuevo más tarde.")
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Supabase REST API calls
    // ─────────────────────────────────────────────────────────────

    /**
     * Busca un código en la tabla Licencia de Supabase.
     * Usa el filtro PostgREST: ?codigo=eq.{code}
     *
     * @return JSONObject con la fila encontrada, o null si no existe.
     */
    private fun fetchLicenseFromSupabase(code: String): JSONObject? {
        val url = "$SUPABASE_URL/rest/v1/$TABLE?codigo=eq.$code&select=*"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return null

        if (!response.isSuccessful) {
            return null
        }

        val array = JSONArray(body)
        return if (array.length() > 0) array.getJSONObject(0) else null
    }

    /**
     * Marca un código como usado en Supabase (usado = true).
     * Usa PATCH con filtro PostgREST: ?codigo=eq.{code}
     *
     * @return true si la operación fue exitosa.
     */
    private fun markCodeAsUsedInSupabase(code: String): Boolean {
        val url = "$SUPABASE_URL/rest/v1/$TABLE?codigo=eq.$code"

        val jsonBody = JSONObject().apply {
            put("usado", true)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .patch(jsonBody.toRequestBody(JSON_TYPE))
            .build()

        val response = client.newCall(request).execute()
        return response.isSuccessful
    }

    // ─────────────────────────────────────────────────────────────
    //  Membresía local (SQLite)
    // ─────────────────────────────────────────────────────────────

    /**
     * Activa la membresía local y extiende la fecha de vencimiento +30 días.
     * @return La nueva fecha de vencimiento en formato yyyy-MM-dd.
     */
    private fun activateLocalMembership(): String {
        val db = dbHelper.writableDatabase

        // Leer fecha de vencimiento actual
        var currentExpiration: Date? = null
        val cursor = db.rawQuery(
            "SELECT fecha_vencimiento FROM membresia WHERE id = 1", null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val dateStr = it.getString(0)
                if (dateStr != null) {
                    currentExpiration = try {
                        dateFormat.parse(dateStr)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }

        // Calcular nueva fecha: max(hoy, fecha_actual) + 30 días
        val today = Date()
        val baseDate = if (currentExpiration != null && currentExpiration!!.after(today)) {
            currentExpiration!!
        } else {
            today
        }
        val calendar = Calendar.getInstance().apply {
            time = baseDate
            add(Calendar.DAY_OF_YEAR, 30)
        }
        val newExpiration = dateFormat.format(calendar.time)

        // Actualizar membresía
        val values = ContentValues().apply {
            put("activo", 1)
            put("fecha_vencimiento", newExpiration)
        }
        db.update("membresia", values, "id = ?", arrayOf("1"))

        return newExpiration
    }

    // ─────────────────────────────────────────────────────────────
    //  Estado de licencia (local)
    // ─────────────────────────────────────────────────────────────

    /**
     * Obtiene el estado actual de la licencia/membresía.
     * Si la fecha de vencimiento ya pasó, desactiva automáticamente.
     */
    fun getLicenseStatus(): LicenseStatus {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT activo, fecha_vencimiento FROM membresia WHERE id = 1", null
        )
        cursor.use {
            if (!it.moveToFirst()) {
                return LicenseStatus(isActive = false, expirationDate = null, daysRemaining = 0)
            }

            val activo = it.getInt(0) == 1
            val fechaStr = it.getString(1)

            if (!activo || fechaStr == null) {
                return LicenseStatus(isActive = false, expirationDate = fechaStr, daysRemaining = 0)
            }

            val expirationDate = try {
                dateFormat.parse(fechaStr)
            } catch (_: Exception) {
                null
            }

            if (expirationDate == null) {
                return LicenseStatus(isActive = false, expirationDate = fechaStr, daysRemaining = 0)
            }

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val diffMs = expirationDate.time - today.time
            val daysRemaining = (diffMs / (1000 * 60 * 60 * 24)).toInt()

            return if (daysRemaining > 0) {
                LicenseStatus(isActive = true, expirationDate = fechaStr, daysRemaining = daysRemaining)
            } else {
                autoDeactivate()
                LicenseStatus(isActive = false, expirationDate = fechaStr, daysRemaining = 0)
            }
        }
    }

    /**
     * Verificación rápida.
     */
    fun isLicenseActive(): Boolean = getLicenseStatus().isActive

    // ─────────────────────────────────────────────────────────────
    //  Utilidades privadas
    // ─────────────────────────────────────────────────────────────

    private fun autoDeactivate() {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("activo", 0)
        }
        db.update("membresia", values, "id = ?", arrayOf("1"))
    }

    private fun calculateDaysRemaining(expirationDateStr: String): Int {
        val expirationDate = try {
            dateFormat.parse(expirationDateStr)
        } catch (_: Exception) {
            return 0
        } ?: return 0

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val diffMs = expirationDate.time - today.time
        return (diffMs / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun extractMonthYear(prefix: String): Pair<String, Int>? {
        for ((_, mesName) in monthNames) {
            if (prefix.startsWith(mesName)) {
                val yearStr = prefix.removePrefix(mesName)
                val year = yearStr.toIntOrNull() ?: continue
                if (year in 2020..2099) {
                    return Pair(mesName, year)
                }
            }
        }
        return null
    }
}
