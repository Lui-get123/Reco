package com.example.repartocobro.data

import android.content.ContentValues
import com.example.repartocobro.model.Collector
import com.example.repartocobro.model.Route
import com.example.repartocobro.model.RouteSummary
import com.example.repartocobro.model.Store
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StoreRepository(private val dbHelper: AppDatabaseHelper) {

    fun getCollectors(): List<Collector> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, name FROM collectors ORDER BY id", null)
        val result = mutableListOf<Collector>()
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    Collector(
                        id = it.getInt(0),
                        name = it.getString(1)
                    )
                )
            }
        }
        return result
    }

    fun getRouteByCollector(collectorId: Int): Route? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, name, collector_id FROM routes WHERE collector_id = ? LIMIT 1",
            arrayOf(collectorId.toString())
        )
        cursor.use {
            return if (it.moveToFirst()) {
                Route(
                    id = it.getInt(0),
                    name = it.getString(1),
                    collectorId = it.getInt(2)
                )
            } else {
                null
            }
        }
    }

    fun getStoresByRoute(routeId: Int): List<Store> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT s.id, s.name, s.route_id, s.delivered_empanadas, s.delivered_deditos,
                   s.sold_empanadas, s.sold_deditos, s.is_collected, s.collection_date,
                   s.is_delivered, s.delivery_date, s.observations,
                   s.pending_debt
            FROM stores s
            WHERE s.route_id = ?
            ORDER BY s.id
            """.trimIndent(),
            arrayOf(routeId.toString())
        )
        val result = mutableListOf<Store>()
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    Store(
                        id = it.getInt(0),
                        name = it.getString(1),
                        routeId = it.getInt(2),
                        deliveredEmpanadas = it.getInt(3),
                        deliveredDeditos = it.getInt(4),
                        soldEmpanadas = it.getInt(5),
                        soldDeditos = it.getInt(6),
                        collectedStatus = it.getInt(7),
                        collectionDate = it.getString(8),
                        isDelivered = it.getInt(9) == 1,
                        deliveryDate = it.getString(10),
                        observations = it.getString(11),
                        pendingDebtTotal = it.getInt(12)
                    )
                )
            }
        }
        return result
    }

    fun updateStoreSales(storeId: Int, soldEmpanadas: Int, soldDeditos: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("sold_empanadas", soldEmpanadas)
            put("sold_deditos", soldDeditos)
        }
        db.update("stores", values, "id = ?", arrayOf(storeId.toString()))
    }

    fun updateStoreDelivered(storeId: Int, deliveredEmpanadas: Int, deliveredDeditos: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("delivered_empanadas", deliveredEmpanadas)
            put("delivered_deditos", deliveredDeditos)
        }
        db.update("stores", values, "id = ?", arrayOf(storeId.toString()))
    }

    fun updateStoreCollected(storeId: Int, collectedStatus: Int, date: String?, observations: String?) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("is_collected", collectedStatus)
            put("collection_date", date)
            put("observations", observations)
        }
        db.update("stores", values, "id = ?", arrayOf(storeId.toString()))
    }

    fun markStoreCollected(storeId: Int) {
        val db = dbHelper.writableDatabase
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val values = ContentValues().apply {
            put("is_collected", 1)
            put("collection_date", currentDate)
        }
        db.update("stores", values, "id = ?", arrayOf(storeId.toString()))
    }

    fun markStoreDelivered(storeId: Int) {
        val db = dbHelper.writableDatabase
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val values = ContentValues().apply {
            put("is_delivered", 1)
            put("delivery_date", currentDate)
        }
        db.update("stores", values, "id = ?", arrayOf(storeId.toString()))
    }

    fun resetRouteStores(routeId: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("sold_empanadas", 0)
            put("sold_deditos", 0)
            put("is_collected", 0)
            putNull("collection_date")
            put("is_delivered", 0)
            putNull("delivery_date")
            putNull("observations")
        }
        db.update("stores", values, "route_id = ?", arrayOf(routeId.toString()))
        // Las deudas pendientes NO se borran al reiniciar la ruta
    }

    fun getRouteSummary(routeId: Int): RouteSummary {
        val stores = getStoresByRoute(routeId)
        return RouteSummary(
            stores = stores,
            totalSoldEmpanadas = stores.sumOf { it.soldEmpanadas },
            totalSoldDeditos = stores.sumOf { it.soldDeditos },
            totalCollectedMoney = stores.sumOf { it.collectedValue },
            totalPendingDebt = stores.sumOf { it.pendingDebtTotal }
        )
    }

    /* ───────────── Deudas Pendientes ───────────── */

    fun addPendingDebt(storeId: Int, amount: Int, observations: String?) {
        val db = dbHelper.writableDatabase
        if (observations != null) {
            db.execSQL(
                "UPDATE stores SET pending_debt = pending_debt + ?, observations = ? WHERE id = ?",
                arrayOf(amount, observations, storeId)
            )
        } else {
            db.execSQL(
                "UPDATE stores SET pending_debt = pending_debt + ? WHERE id = ?",
                arrayOf(amount, storeId)
            )
        }
    }

    fun markAllDebtsAsPaid(storeId: Int) {
        val db = dbHelper.writableDatabase
        db.execSQL("UPDATE stores SET pending_debt = 0 WHERE id = ?", arrayOf(storeId))
    }
}

