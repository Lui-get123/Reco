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
            SELECT id, name, route_id, delivered_empanadas, delivered_deditos,
                   sold_empanadas, sold_deditos, is_collected, collection_date,
                   is_delivered, delivery_date
            FROM stores
            WHERE route_id = ?
            ORDER BY id
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
                        isCollected = it.getInt(7) == 1,
                        collectionDate = it.getString(8),
                        isDelivered = it.getInt(9) == 1,
                        deliveryDate = it.getString(10)
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
            put("delivered_empanadas", 0)
            put("delivered_deditos", 0)
            put("sold_empanadas", 0)
            put("sold_deditos", 0)
            put("is_collected", 0)
            putNull("collection_date")
            put("is_delivered", 0)
            putNull("delivery_date")
        }
        db.update("stores", values, "route_id = ?", arrayOf(routeId.toString()))
    }

    fun getRouteSummary(routeId: Int): RouteSummary {
        val stores = getStoresByRoute(routeId)
        return RouteSummary(
            stores = stores,
            totalSoldEmpanadas = stores.sumOf { it.soldEmpanadas },
            totalSoldDeditos = stores.sumOf { it.soldDeditos },
            totalCollectedMoney = stores.sumOf { it.collectedValue }
        )
    }
}
