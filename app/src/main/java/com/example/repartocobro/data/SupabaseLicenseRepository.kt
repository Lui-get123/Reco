package com.example.repartocobro.data

import android.content.ContentValues
import com.example.repartocobro.BuildConfig
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
 * 1. Validar formato: serial alfanumérico de 12 caracteres.
 * 2. Consultar Supabase para verificar que el código exista.
 * 3. Verificar que el campo "usado" sea false.
 * 4. Si es válido: marcar como usado en Supabase + activar membresía local (+15 días).
 * 5. Si no es válido: retornar error descriptivo.
 */
class SupabaseLicenseRepository(private val dbHelper: AppDatabaseHelper) {

    companion object {
        // Credenciales inyectadas desde local.properties vía BuildConfig
        private val SUPABASE_URL = BuildConfig.SUPABASE_URL
        private val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY
        private const val TABLE = "licencia"
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val SERIAL_LENGTH = 12
        private const val DAYS_PER_LICENSE = 15
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
     * Valida un serial de 12 caracteres contra Supabase (tabla Licencia) y si es válido,
     * lo marca como usado y activa la membresía local (+15 días).
     *
     * DEBE ejecutarse en un hilo de fondo (coroutine / Dispatchers.IO).
     */
    fun redeemCode(code: String): RedeemResult {
        val trimmed = code.trim().uppercase()

        // 1. Validar formato: serial alfanumérico de 12 caracteres
        if (trimmed.length != SERIAL_LENGTH) {
            return RedeemResult.Error("El código debe tener exactamente $SERIAL_LENGTH caracteres.")
        }

        if (!trimmed.all { it.isLetterOrDigit() }) {
            return RedeemResult.Error("El código solo debe contener letras y números.")
        }

        // 2. Consultar Supabase: buscar el código en la tabla Licencia
        try {
            val row = fetchLicenseFromSupabase(trimmed)
                ?: return RedeemResult.Error("Código no encontrado.\nVerifica que el código sea correcto.")

            // 3. Verificar que el campo "usado" sea false
            val usado = row.optBoolean("usado", true)
            if (usado) {
                return RedeemResult.Error("Este código ya fue utilizado.\nSolicita un código nuevo.")
            }

            // 4. Marcar como usado en Supabase
            val marked = markCodeAsUsedInSupabase(trimmed)
            if (!marked) {
                return RedeemResult.Error("Error al activar la licencia.\nIntenta de nuevo más tarde.")
            }

            // 5. Activar membresía local (+15 días)
            val newExpiration = activateLocalMembership()

            return RedeemResult.Success(
                daysAdded = DAYS_PER_LICENSE,
                newExpiration = newExpiration
            )

        } catch (e: java.net.UnknownHostException) {
            return RedeemResult.Error("Sin conexión a internet.\nVerifica tu conexión e intenta de nuevo.")
        } catch (e: java.net.SocketTimeoutException) {
            return RedeemResult.Error("Tiempo de espera agotado.\nVerifica tu conexión e intenta de nuevo.")
        } catch (e: Exception) {
            return RedeemResult.Error("Error de conexión: ${e.localizedMessage ?: "desconocido"}\nIntenta de nuevo más tarde.")
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
     * Activa la membresía local y extiende la fecha de vencimiento +15 días (quincenal).
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

        // Calcular nueva fecha: max(hoy, fecha_actual) + 15 días
        val today = Date()
        val baseDate = if (currentExpiration != null && currentExpiration!!.after(today)) {
            currentExpiration!!
        } else {
            today
        }
        val calendar = Calendar.getInstance().apply {
            time = baseDate
            add(Calendar.DAY_OF_YEAR, DAYS_PER_LICENSE)
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

}
