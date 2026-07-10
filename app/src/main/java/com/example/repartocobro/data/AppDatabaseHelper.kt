package com.example.repartocobro.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS collectors (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS routes (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                collector_id INTEGER NOT NULL,
                FOREIGN KEY(collector_id) REFERENCES collectors(id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                price INTEGER NOT NULL,
                icon_name TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS stores (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                route_id INTEGER NOT NULL,
                is_collected INTEGER NOT NULL DEFAULT 0,
                collection_date TEXT,
                is_delivered INTEGER NOT NULL DEFAULT 0,
                delivery_date TEXT,
                observations TEXT,
                pending_debt INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(route_id) REFERENCES routes(id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS store_products (
                store_id INTEGER NOT NULL,
                product_id INTEGER NOT NULL,
                delivered_quantity INTEGER NOT NULL DEFAULT 0,
                sold_quantity INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (store_id, product_id),
                FOREIGN KEY(store_id) REFERENCES stores(id) ON DELETE CASCADE,
                FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        createMembershipTable(db)
        seedTestData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            createMembershipTable(db)
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE stores ADD COLUMN is_delivered INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE stores ADD COLUMN delivery_date TEXT")
        }
        if (oldVersion < 8) {
            db.execSQL("DROP TABLE IF EXISTS licencia")
            createMembershipTable(db)
            db.execSQL("UPDATE membresia SET activo = 0, fecha_vencimiento = NULL WHERE id = 1")
        }
        if (oldVersion < 9) {
            db.execSQL("UPDATE collectors SET name = 'Mateo' WHERE id = 1")
            db.execSQL("UPDATE collectors SET name = 'Daison' WHERE id = 2")
        }
        if (oldVersion < 10) {
            db.execSQL("DELETE FROM stores")
            db.execSQL("DELETE FROM routes")
            seedTestData(db)
        }
        if (oldVersion < 11) {
            db.execSQL("ALTER TABLE stores ADD COLUMN observations TEXT")
        }
        if (oldVersion < 12) {
            db.execSQL("UPDATE stores SET name = 'Esneider' WHERE name = 'Sueider' AND route_id = 1")
            db.execSQL("UPDATE stores SET name = 'Loma' WHERE name = 'Laura' AND route_id = 1")
        }
        if (oldVersion < 13) {
            // Already handled by older resets if running from an old version
        }
        if (oldVersion < 14) {
            db.execSQL("UPDATE stores SET name = 'Roji' WHERE name = 'Rosi' AND route_id = 2")
            db.execSQL("UPDATE stores SET name = 'Ferre' WHERE name = 'felipe' AND route_id = 2")
            db.execSQL("UPDATE stores SET name = 'Muelle' WHERE name = 'Nello' AND route_id = 2")
            db.execSQL("UPDATE stores SET name = 'Loma' WHERE name = 'Loma 2' AND route_id = 2")
            db.execSQL("DELETE FROM stores WHERE name = 'Divino' AND route_id = 2")
        }
        if (oldVersion < 15) {
            db.execSQL("UPDATE stores SET name = 'Chucho' WHERE id = 42 AND route_id = 2")
            db.execSQL("UPDATE stores SET name = 'Nelly' WHERE id = 48 AND route_id = 2")
            db.execSQL("UPDATE stores SET name = 'Futuro' WHERE id = 54 AND route_id = 2")
            db.execSQL("UPDATE stores SET name = 'Punto' WHERE id = 66 AND route_id = 2")
            for (oldId in 64..76) {
                db.execSQL("UPDATE stores SET id = ${oldId - 1} WHERE id = $oldId AND route_id = 2")
            }
            for (oldId in 75 downTo 59) {
                db.execSQL("UPDATE stores SET id = ${oldId + 1} WHERE id = $oldId AND route_id = 2")
            }
            val cv = ContentValues().apply {
                put("id", 59)
                put("name", "5 Boca")
                put("route_id", 2)
                put("delivered_empanadas", 0)
                put("delivered_deditos", 0)
                put("sold_empanadas", 0)
                put("sold_deditos", 0)
                put("is_collected", 0)
            }
            db.insert("stores", null, cv)
        }
        if (oldVersion < 16) {
            // createPendingDebtsTable(db) -- Deleted in v17
        }
        if (oldVersion < 17) {
            try { db.execSQL("ALTER TABLE stores ADD COLUMN pending_debt INTEGER NOT NULL DEFAULT 0") } catch(e:Exception){}
            try {
                db.execSQL("UPDATE stores SET pending_debt = COALESCE((SELECT SUM(amount) FROM pending_debts WHERE pending_debts.store_id = stores.id AND pending_debts.is_paid = 0), 0)")
            } catch (e: Exception) {}
            db.execSQL("DROP TABLE IF EXISTS pending_debts")
        }
        if (oldVersion < 22) {
            val cv = ContentValues().apply {
                put("id", 80)
                put("name", "Pacto con Dios")
                put("route_id", 1)
                put("delivered_empanadas", 5)
                put("delivered_deditos", 5)
                put("sold_empanadas", 0)
                put("sold_deditos", 0)
                put("is_collected", 0)
                put("is_delivered", 0)
                put("pending_debt", 0)
            }
            db.insert("stores", null, cv)
        }
        if (oldVersion < 23) {
            // 1. Crear tabla de productos
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    price INTEGER NOT NULL,
                    icon_name TEXT
                )
                """.trimIndent()
            )
            // 2. Insertar productos por defecto
            db.execSQL("INSERT INTO products (id, name, price, icon_name) VALUES (1, 'Empanadas', 1100, 'Package')")
            db.execSQL("INSERT INTO products (id, name, price, icon_name) VALUES (2, 'Deditos', 900, 'Package')")

            // 3. Crear tabla de store_products
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS store_products (
                    store_id INTEGER NOT NULL,
                    product_id INTEGER NOT NULL,
                    delivered_quantity INTEGER NOT NULL DEFAULT 0,
                    sold_quantity INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (store_id, product_id),
                    FOREIGN KEY(store_id) REFERENCES stores(id) ON DELETE CASCADE,
                    FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            // 4. Migrar datos de stores a store_products
            db.execSQL(
                """
                INSERT INTO store_products (store_id, product_id, delivered_quantity, sold_quantity)
                SELECT id, 1, delivered_empanadas, sold_empanadas FROM stores
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO store_products (store_id, product_id, delivered_quantity, sold_quantity)
                SELECT id, 2, delivered_deditos, sold_deditos FROM stores
                """.trimIndent()
            )

            // 5. Eliminar columnas antiguas de stores creando una tabla nueva
            db.execSQL(
                """
                CREATE TABLE stores_v2 (
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    route_id INTEGER NOT NULL,
                    is_collected INTEGER NOT NULL DEFAULT 0,
                    collection_date TEXT,
                    is_delivered INTEGER NOT NULL DEFAULT 0,
                    delivery_date TEXT,
                    observations TEXT,
                    pending_debt INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(route_id) REFERENCES routes(id)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO stores_v2 (id, name, route_id, is_collected, collection_date, is_delivered, delivery_date, observations, pending_debt)
                SELECT id, name, route_id, is_collected, collection_date, is_delivered, delivery_date, observations, pending_debt FROM stores
                """.trimIndent()
            )
            db.execSQL("DROP TABLE stores")
            db.execSQL("ALTER TABLE stores_v2 RENAME TO stores")
        }
    }

    private fun createMembershipTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS membresia (
                id INTEGER PRIMARY KEY,
                activo INTEGER NOT NULL DEFAULT 0,
                fecha_vencimiento TEXT
            )
            """.trimIndent()
        )
        db.execSQL("INSERT OR IGNORE INTO membresia (id, activo, fecha_vencimiento) VALUES (1, 0, NULL)")
    }

    private fun seedTestData(db: SQLiteDatabase) {
        // La base de datos iniciará completamente limpia.
        // Los usuarios podrán agregar sus productos, cobradores y tiendas desde el Panel de Administración.
    }

    private fun insertCollector(db: SQLiteDatabase, id: Int, name: String) {
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
        }
        db.insert("collectors", null, values)
    }

    private fun insertRoute(db: SQLiteDatabase, id: Int, name: String, collectorId: Int) {
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
            put("collector_id", collectorId)
        }
        db.insert("routes", null, values)
    }

    private fun insertStore(
        db: SQLiteDatabase,
        id: Int,
        name: String,
        routeId: Int,
        deliveredEmpanadas: Int,
        deliveredDeditos: Int
    ) {
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
            put("route_id", routeId)
            put("is_collected", 0)
        }
        db.insert("stores", null, values)
        
        // Add default products
        val cvE = ContentValues().apply {
            put("store_id", id)
            put("product_id", 1)
            put("delivered_quantity", deliveredEmpanadas)
            put("sold_quantity", 0)
        }
        db.insert("store_products", null, cvE)
        
        val cvD = ContentValues().apply {
            put("store_id", id)
            put("product_id", 2)
            put("delivered_quantity", deliveredDeditos)
            put("sold_quantity", 0)
        }
        db.insert("store_products", null, cvD)
    }

    companion object {
        private const val DB_NAME = "reparto_cobro.db"
        private const val DB_VERSION = 23
    }
}
