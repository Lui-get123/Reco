package com.example.repartocobro.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.repartocobro.model.Collector
import com.example.repartocobro.model.Product
import com.example.repartocobro.model.Route
import com.example.repartocobro.model.RouteSummary
import com.example.repartocobro.model.Store
import com.example.repartocobro.model.StoreProduct
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StoreRepository(private val dbHelper: AppDatabaseHelper) {

    fun getProducts(): List<Product> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, name, price, icon_name FROM products ORDER BY id", null)
        val result = mutableListOf<Product>()
        cursor.use {
            while (it.moveToNext()) {
                result.add(Product(it.getInt(0), it.getString(1), it.getInt(2), it.getString(3)))
            }
        }
        return result
    }

    fun getCollectors(): List<Collector> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, name FROM collectors ORDER BY id", null)
        val result = mutableListOf<Collector>()
        cursor.use {
            while (it.moveToNext()) {
                result.add(Collector(id = it.getInt(0), name = it.getString(1)))
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
                Route(id = it.getInt(0), name = it.getString(1), collectorId = it.getInt(2))
            } else {
                null
            }
        }
    }

    fun getStoresByRoute(routeId: Int): List<Store> {
        val db = dbHelper.readableDatabase
        
        val productsMap = getProducts().associateBy { it.id }
        
        val storesCursor = db.rawQuery(
            """
            SELECT id, name, route_id, is_collected, collection_date,
                   is_delivered, delivery_date, observations, pending_debt
            FROM stores WHERE route_id = ? ORDER BY id
            """.trimIndent(),
            arrayOf(routeId.toString())
        )
        
        val storeProductsCursor = db.rawQuery(
            """
            SELECT sp.store_id, sp.product_id, sp.delivered_quantity, sp.sold_quantity
            FROM store_products sp
            JOIN stores s ON s.id = sp.store_id
            WHERE s.route_id = ?
            """.trimIndent(),
            arrayOf(routeId.toString())
        )
        
        val storeProductsMap = mutableMapOf<Int, MutableList<StoreProduct>>()
        storeProductsCursor.use {
            while(it.moveToNext()) {
                val storeId = it.getInt(0)
                val productId = it.getInt(1)
                val product = productsMap[productId]
                if (product != null) {
                    val sp = StoreProduct(storeId, product, it.getInt(2), it.getInt(3))
                    storeProductsMap.getOrPut(storeId) { mutableListOf() }.add(sp)
                }
            }
        }
        
        val stores = mutableListOf<Store>()
        storesCursor.use {
            while(it.moveToNext()) {
                val storeId = it.getInt(0)
                var spList = storeProductsMap[storeId]
                
                if (spList == null) {
                    spList = productsMap.values.map { StoreProduct(storeId, it, 0, 0) }.toMutableList()
                } else if (spList.size < productsMap.size) {
                    val existingIds = spList.map { it.product.id }.toSet()
                    val missing = productsMap.values.filter { it.id !in existingIds }.map { StoreProduct(storeId, it, 0, 0) }
                    spList.addAll(missing)
                }
                spList.sortBy { it.product.id }

                stores.add(Store(
                    id = storeId,
                    name = it.getString(1),
                    routeId = it.getInt(2),
                    products = spList,
                    collectedStatus = it.getInt(3),
                    collectionDate = it.getString(4),
                    isDelivered = it.getInt(5) == 1,
                    deliveryDate = it.getString(6),
                    observations = it.getString(7),
                    pendingDebtTotal = it.getInt(8)
                ))
            }
        }
        return stores
    }

    fun updateStoreProduct(storeId: Int, productId: Int, delivered: Int, sold: Int) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("store_id", storeId)
            put("product_id", productId)
            put("delivered_quantity", delivered)
            put("sold_quantity", sold)
        }
        db.insertWithOnConflict("store_products", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
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
            put("is_collected", 0)
            putNull("collection_date")
            put("is_delivered", 0)
            putNull("delivery_date")
            putNull("observations")
        }
        db.update("stores", values, "route_id = ?", arrayOf(routeId.toString()))
        
        db.execSQL(
            "UPDATE store_products SET sold_quantity = 0 WHERE store_id IN (SELECT id FROM stores WHERE route_id = ?)",
            arrayOf(routeId)
        )
    }

    fun getRouteSummary(routeId: Int): RouteSummary {
        val stores = getStoresByRoute(routeId)
        return RouteSummary(
            stores = stores,
            totalCollectedMoney = stores.sumOf { it.collectedValue },
            totalPendingDebt = stores.sumOf { it.pendingDebtTotal }
        )
    }

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

    // --- CRUD for Admin ---
    fun addProduct(name: String, price: Int, iconName: String?) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", name)
            put("price", price)
            put("icon_name", iconName)
        }
        db.insert("products", null, cv)
    }

    fun updateProduct(id: Int, name: String, price: Int, iconName: String?) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", name)
            put("price", price)
            put("icon_name", iconName)
        }
        db.update("products", cv, "id = ?", arrayOf(id.toString()))
    }

    fun deleteProduct(id: Int) {
        val db = dbHelper.writableDatabase
        db.delete("products", "id = ?", arrayOf(id.toString()))
    }

    fun addCollector(name: String) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", name)
        }
        val collectorId = db.insert("collectors", null, cv).toInt()
        
        // Automáticamente crearle una ruta
        val routeCv = ContentValues().apply {
            put("name", "Ruta de $name")
            put("collector_id", collectorId)
        }
        db.insert("routes", null, routeCv)
    }

    fun deleteCollector(id: Int) {
        val db = dbHelper.writableDatabase
        // Also delete the route associated
        db.delete("routes", "collector_id = ?", arrayOf(id.toString()))
        db.delete("collectors", "id = ?", arrayOf(id.toString()))
    }

    fun addStore(name: String, routeId: Int) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", name)
            put("route_id", routeId)
        }
        db.insert("stores", null, cv)
    }
    
    fun updateStoreName(id: Int, name: String) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", name)
        }
        db.update("stores", cv, "id = ?", arrayOf(id.toString()))
    }

    fun deleteStore(id: Int) {
        val db = dbHelper.writableDatabase
        db.delete("stores", "id = ?", arrayOf(id.toString()))
    }
}

