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
            CREATE TABLE IF NOT EXISTS stores (
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
                observations TEXT,
                pending_debt INTEGER NOT NULL DEFAULT 0,
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
        if (oldVersion < 10) {
            // v10: Limpiar tiendas y rutas antiguas para insertar las nuevas listas oficiales
            db.execSQL("DELETE FROM stores")
            db.execSQL("DELETE FROM routes")
            seedTestData(db)
        }
        if (oldVersion < 11) {
            // v11: Añadir columna de observaciones
            db.execSQL("ALTER TABLE stores ADD COLUMN observations TEXT")
        }
        if (oldVersion < 12) {
            // v12: Corregir nombres de tiendas en la ruta de Daison
            db.execSQL("UPDATE stores SET name = 'Esneider' WHERE name = 'Sueider' AND route_id = 1")
            db.execSQL("UPDATE stores SET name = 'Loma' WHERE name = 'Laura' AND route_id = 1")
        }
        if (oldVersion < 13) {
            // v13: Forzar recreación completa para asegurar consistencia de columnas (observations, etc)
            db.execSQL("DROP TABLE IF EXISTS stores")
            db.execSQL("DROP TABLE IF EXISTS routes")
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
                CREATE TABLE IF NOT EXISTS stores (
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
                    observations TEXT,
                    pending_debt INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(route_id) REFERENCES routes(id)
                )
                """.trimIndent()
            )
            createMembershipTable(db)
            seedTestData(db)
        }
        if (oldVersion < 14) {
            // v14: Renombrar tiendas de Mateo y eliminar Divino
            db.execSQL("UPDATE stores SET name = 'Roji' WHERE name = 'Rosi' AND route_id = 2")
            db.execSQL("UPDATE stores SET name = 'Ferre' WHERE name = 'felipe' AND route_id = 2")
            db.execSQL("UPDATE stores SET name = 'Muelle' WHERE name = 'Nello' AND route_id = 2")
            db.execSQL("UPDATE stores SET name = 'Loma' WHERE name = 'Loma 2' AND route_id = 2")
            db.execSQL("DELETE FROM stores WHERE name = 'Divino' AND route_id = 2")
        }
        if (oldVersion < 15) {
            // v15: Ajustes ruta Mateo
            // Renombrar tiendas (usando IDs originales, antes del shift)
            db.execSQL("UPDATE stores SET name = 'Chucho' WHERE id = 42 AND route_id = 2")
            db.execSQL("UPDATE stores SET name = 'Nelly' WHERE id = 48 AND route_id = 2")
            db.execSQL("UPDATE stores SET name = 'Futuro' WHERE id = 54 AND route_id = 2")
            db.execSQL("UPDATE stores SET name = 'Punto' WHERE id = 66 AND route_id = 2")
            // Desplazar IDs para abrir espacio al id 59
            // Original: 59,60,61,62, (no 63), 64,65,66,67,68,69,70,71,72,73,74,75,76
            // Primero cerrar hueco del 63: bajar 64→63, 65→64, ..., 76→75
            for (oldId in 64..76) {
                db.execSQL("UPDATE stores SET id = ${oldId - 1} WHERE id = $oldId AND route_id = 2")
            }
            // Ahora los IDs son: 59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75
            // Subir todos de 75→59 (+1) para dejar libre el 59
            for (oldId in 75 downTo 59) {
                db.execSQL("UPDATE stores SET id = ${oldId + 1} WHERE id = $oldId AND route_id = 2")
            }
            // Ahora los IDs son: 60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76
            // Insertar 5 Boca con id 59
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
            // v16: Tabla de deudas pendientes (pagos al día siguiente)
            createPendingDebtsTable(db)
        }
        if (oldVersion < 17) {
            // v17: Unificar deudas pendientes en la tabla stores
            db.execSQL("ALTER TABLE stores ADD COLUMN pending_debt INTEGER NOT NULL DEFAULT 0")
            // Migrar la deuda existente
            try {
                db.execSQL("""
                    UPDATE stores 
                    SET pending_debt = COALESCE((
                        SELECT SUM(amount) FROM pending_debts 
                        WHERE pending_debts.store_id = stores.id AND pending_debts.is_paid = 0
                    ), 0)
                """.trimIndent())
            } catch (e: Exception) {
                // Ignore if pending_debts doesn't exist for some reason
            }
            db.execSQL("DROP TABLE IF EXISTS pending_debts")
        }
        if (oldVersion < 22) {
            // v22: Añadir "Pacto con Dios" en ruta 1 (Daison)
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
    }

    /**
     * Crea solo la tabla de membresía local (estado de la licencia).
     * Los códigos ahora se validan contra Supabase.
     */
    private fun createPendingDebtsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pending_debts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                store_id INTEGER NOT NULL,
                route_id INTEGER NOT NULL,
                empanadas_sold INTEGER NOT NULL DEFAULT 0,
                deditos_sold INTEGER NOT NULL DEFAULT 0,
                amount INTEGER NOT NULL,
                created_date TEXT NOT NULL,
                is_paid INTEGER NOT NULL DEFAULT 0,
                paid_date TEXT,
                observations TEXT,
                FOREIGN KEY(store_id) REFERENCES stores(id),
                FOREIGN KEY(route_id) REFERENCES routes(id)
            )
            """.trimIndent()
        )
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
        // Fila inicial: membresía inactiva
        db.execSQL(
            "INSERT OR IGNORE INTO membresia (id, activo, fecha_vencimiento) VALUES (1, 0, NULL)"
        )
    }

    private fun seedTestData(db: SQLiteDatabase) {
        insertCollector(db, 1, "Mateo")
        insertCollector(db, 2, "Daison")

        insertRoute(db, 1, "Ruta Daison", 2)
        insertRoute(db, 2, "Ruta Mateo", 1)

        // ── RUTA DAISON (Collector 2) ──
        insertStore(db, 1, "el Negro", 1, 10, 5, 0, 0)
        insertStore(db, 2, "Esneider", 1, 5, 5, 0, 0)
        insertStore(db, 3, "La nueva de la 12", 1, 10, 0, 0, 0)
        insertStore(db, 4, "Fabio", 1, 10, 5, 0, 0)
        insertStore(db, 5, "El Gordo", 1, 5, 5, 0, 0)
        insertStore(db, 6, "AK", 1, 20, 0, 0, 0)
        insertStore(db, 7, "Agrado", 1, 10, 0, 0, 0)
        insertStore(db, 8, "Samuel", 1, 15, 5, 0, 0)
        insertStore(db, 9, "Fortuna", 1, 10, 10, 0, 0)
        insertStore(db, 10, "olimpico", 1, 5, 5, 0, 0)
        insertStore(db, 11, "Victoria", 1, 15, 10, 0, 0)
        insertStore(db, 12, "Lider", 1, 15, 15, 0, 0)
        insertStore(db, 13, "Mañe", 1, 25, 0, 0, 0)
        insertStore(db, 14, "Angeles", 1, 15, 10, 0, 0)
        insertStore(db, 15, "Palmera", 1, 15, 0, 0, 0)
        insertStore(db, 16, "Esq. Caliente", 1, 5, 5, 0, 0)
        insertStore(db, 17, "El Cacha", 1, 5, 5, 0, 0)
        insertStore(db, 18, "Gustavo", 1, 15, 10, 0, 0)
        insertStore(db, 19, "Kiosco", 1, 5, 5, 0, 0)
        insertStore(db, 20, "La nueva del arroyo", 1, 10, 5, 0, 0)
        insertStore(db, 21, "Costeña", 1, 10, 5, 0, 0)
        insertStore(db, 22, "P. Verde", 1, 15, 10, 0, 0)
        insertStore(db, 23, "Ocañera", 1, 5, 5, 0, 0)
        insertStore(db, 24, "JV", 1, 16, 5, 0, 0)
        insertStore(db, 25, "Loma", 1, 15, 5, 0, 0)
        insertStore(db, 26, "Panaderia", 1, 20, 10, 0, 0)
        insertStore(db, 27, "Medellin", 1, 20, 10, 0, 0)
        insertStore(db, 28, "Postobon", 1, 15, 5, 0, 0)
        insertStore(db, 29, "Silencio", 1, 15, 10, 0, 0)
        insertStore(db, 30, "Piedra", 1, 15, 10, 0, 0)
        insertStore(db, 31, "Maria", 1, 15, 15, 0, 0)
        insertStore(db, 32, "5 a 0", 1, 20, 15, 0, 0)
        insertStore(db, 33, "La nena", 1, 20, 15, 0, 0)
        insertStore(db, 34, "24 Hora", 1, 5, 5, 0, 0)
        insertStore(db, 35, "San Luis", 1, 25, 0, 0, 0)
        insertStore(db, 80, "Pacto con Dios", 1, 5, 5, 0, 0)

        // ── RUTA MATEO (Collector 1) ──
        insertStore(db, 36, "Lucio", 2, 5, 5, 0, 0)
        insertStore(db, 37, "Esq. Lucio", 2, 10, 10, 0, 0)
        insertStore(db, 38, "Roji", 2, 10, 10, 0, 0)
        insertStore(db, 39, "Ferre", 2, 5, 5, 0, 0)
        insertStore(db, 40, "Ricardo", 2, 10, 10, 0, 0)
        insertStore(db, 41, "Nueva", 2, 5, 5, 0, 0)
        insertStore(db, 42, "Chucho", 2, 15, 0, 0, 0)
        insertStore(db, 43, "Nury", 2, 10, 5, 0, 0)
        insertStore(db, 44, "Mingo", 2, 5, 5, 0, 0)
        insertStore(db, 45, "Esq. Mingo", 2, 10, 10, 0, 0)
        insertStore(db, 46, "Mario", 2, 5, 15, 0, 0)
        insertStore(db, 47, "Pluma", 2, 10, 10, 0, 0)
        insertStore(db, 48, "Nelly", 2, 5, 5, 0, 0)
        insertStore(db, 49, "Mery", 2, 5, 5, 0, 0)
        insertStore(db, 50, "Muelle", 2, 5, 5, 0, 0)
        insertStore(db, 51, "Insuperable", 2, 5, 5, 0, 0)
        insertStore(db, 52, "Golazo", 2, 15, 5, 0, 0)
        insertStore(db, 53, "Virgen", 2, 15, 0, 0, 0)
        insertStore(db, 54, "Futuro", 2, 10, 10, 0, 0)
        insertStore(db, 55, "Paisa", 2, 5, 5, 0, 0)
        insertStore(db, 56, "Loma", 2, 15, 0, 0, 0)
        insertStore(db, 57, "Risota", 2, 10, 5, 0, 0)
        insertStore(db, 58, "San Juan", 2, 10, 5, 0, 0)
        insertStore(db, 59, "5 Boca", 2, 0, 0, 0, 0)
        insertStore(db, 60, "Esq. Movimiento", 2, 10, 10, 0, 0)
        insertStore(db, 61, "Recuerdo", 2, 10, 10, 0, 0)
        insertStore(db, 62, "Richard", 2, 15, 10, 0, 0)
        insertStore(db, 63, "Sagrado", 2, 10, 5, 0, 0)
        insertStore(db, 64, "Economico", 2, 15, 10, 0, 0)
        insertStore(db, 65, "Esmeralda", 2, 10, 5, 0, 0)
        insertStore(db, 66, "Punto", 2, 10, 5, 0, 0)
        insertStore(db, 67, "Roja", 2, 15, 0, 0, 0)
        insertStore(db, 68, "Ancla", 2, 5, 5, 0, 0)
        insertStore(db, 69, "Bonanza", 2, 10, 0, 0, 0)
        insertStore(db, 70, "Capilla", 2, 10, 10, 0, 0)
        insertStore(db, 71, "Cañonazo", 2, 5, 5, 0, 0)
        insertStore(db, 72, "Vistamar", 2, 15, 0, 0, 0)
        insertStore(db, 73, "Marysol", 2, 10, 0, 0, 0)
        insertStore(db, 74, "Barato", 2, 5, 5, 0, 0)
        insertStore(db, 75, "Jimena", 2, 5, 10, 0, 0)
        insertStore(db, 76, "Hugo", 2, 15, 0, 0, 0)
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
        private const val DB_VERSION = 22
    }
}
