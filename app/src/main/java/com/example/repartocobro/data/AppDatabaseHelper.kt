package com.example.repartocobro.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Base de datos local. Los códigos de licencia ahora viven en Supabase,
 * aquí solo se mantiene la tabla 'membresia' para guardar el estado local.
 */

class AppDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE collectors (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE routes (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                collector_id INTEGER NOT NULL,
                FOREIGN KEY(collector_id) REFERENCES collectors(id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE stores (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                route_id INTEGER NOT NULL,
                delivered_empanadas INTEGER NOT NULL,
                delivered_deditos INTEGER NOT NULL,
                sold_empanadas INTEGER NOT NULL DEFAULT 0,
                sold_deditos INTEGER NOT NULL DEFAULT 0,
                is_collected INTEGER NOT NULL DEFAULT 0,
                collection_date TEXT,
                is_delivered INTEGER NOT NULL DEFAULT 0,
                delivery_date TEXT,
                FOREIGN KEY(route_id) REFERENCES routes(id)
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
            // v8: Códigos migrados a Supabase → borrar tabla local y resetear membresía
            db.execSQL("DROP TABLE IF EXISTS licencia")
            createMembershipTable(db)
            // Forzar membresía inactiva para que el usuario valide con Supabase
            db.execSQL("UPDATE membresia SET activo = 0, fecha_vencimiento = NULL WHERE id = 1")
        }
        if (oldVersion < 9) {
            // v9: Actualizar nombres de recolectores a Mateo y Daison
            db.execSQL("UPDATE collectors SET name = 'Mateo' WHERE id = 1")
            db.execSQL("UPDATE collectors SET name = 'Daison' WHERE id = 2")
        }
    }

    /**
     * Crea solo la tabla de membresía local (estado de la licencia).
     * Los códigos ahora se validan contra Supabase.
     */
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
        // Fila inicial: membresía inactiva
        db.execSQL(
            "INSERT OR IGNORE INTO membresia (id, activo, fecha_vencimiento) VALUES (1, 0, NULL)"
        )
    }

    private fun seedTestData(db: SQLiteDatabase) {
        insertCollector(db, 1, "Mateo")
        insertCollector(db, 2, "Daison")

        insertRoute(db, 1, "Ruta 1", 1)
        insertRoute(db, 2, "Ruta 2", 2)

        insertStore(
            db = db,
            id = 1,
            name = "Tienda La Esquina",
            routeId = 1,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 2,
            name = "Tienda El Centro",
            routeId = 1,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 3,
            name = "Tienda La Villa",
            routeId = 1,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 4,
            name = "Tienda El Parque",
            routeId = 1,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 5,
            name = "Tienda La Plazuela",
            routeId = 1,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 6,
            name = "Tienda El Bosque",
            routeId = 1,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 7,
            name = "Tienda La Montaña",
            routeId = 1,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 8,
            name = "Tienda El Rincón",
            routeId = 1,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 9,
            name = "Tienda La Aurora",
            routeId = 1,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 10,
            name = "Tienda El Mirador",
            routeId = 1,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 11,
            name = "Tienda San Jose",
            routeId = 2,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 12,
            name = "Tienda Las Flores",
            routeId = 2,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 13,
            name = "Tienda El Nogal",
            routeId = 2,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 14,
            name = "Tienda La Palma",
            routeId = 2,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 15,
            name = "Tienda El Porvenir",
            routeId = 2,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 16,
            name = "Tienda La Esperanza",
            routeId = 2,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 17,
            name = "Tienda El Manantial",
            routeId = 2,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 18,
            name = "Tienda La Ceiba",
            routeId = 2,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 19,
            name = "Tienda El Triunfo",
            routeId = 2,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
        insertStore(
            db = db,
            id = 20,
            name = "Tienda La Victoria",
            routeId = 2,
            deliveredEmpanadas = 0,
            deliveredDeditos = 0,
            soldEmpanadas = 0,
            soldDeditos = 0
        )
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
        deliveredDeditos: Int,
        soldEmpanadas: Int,
        soldDeditos: Int
    ) {
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
            put("route_id", routeId)
            put("delivered_empanadas", deliveredEmpanadas)
            put("delivered_deditos", deliveredDeditos)
            put("sold_empanadas", soldEmpanadas)
            put("sold_deditos", soldDeditos)
            put("is_collected", 0)
        }
        db.insert("stores", null, values)
    }

    companion object {
        private const val DB_NAME = "reparto_cobro.db"
        private const val DB_VERSION = 9
    }
}
