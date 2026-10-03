package com.ganadopro.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ganadopro.data.dao.*
import com.ganadopro.data.entities.*

@Database(
    entities = [
        Animal::class,
        HistorialMedico::class,
        Vacuna::class,
        Reproduccion::class,
        Alimentacion::class,
        ControlPeso::class,
        Produccion::class,
        Venta::class,
        Gasto::class,
        AlertaEnviada::class
    ],
    version = 4,
    exportSchema = true
)
abstract class GanaderaDatabase : RoomDatabase() {

    abstract fun animalDao(): AnimalDao
    abstract fun historialMedicoDao(): HistorialMedicoDao
    abstract fun vacunaDao(): VacunaDao
    abstract fun reproduccionDao(): ReproduccionDao
    abstract fun alimentacionDao(): AlimentacionDao
    abstract fun controlPesoDao(): ControlPesoDao
    abstract fun produccionDao(): ProduccionDao
    abstract fun ventaDao(): VentaDao
    abstract fun gastoDao(): GastoDao
    abstract fun alertaEnviadaDao(): AlertaEnviadaDao

    companion object {
        @Volatile
        private var INSTANCE: GanaderaDatabase? = null

        // CREATE TABLE / CREATE INDEX generados a partir de la entidad
        // AlertaEnviada (ver data/entities/AlertaEnviada.kt). Solo agrega
        // una tabla nueva, no modifica ninguna existente.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `alertas_enviadas` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tipo` TEXT NOT NULL,
                        `entidadId` INTEGER NOT NULL,
                        `fechaEnviada` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_alertas_enviadas_tipo_entidadId`
                    ON `alertas_enviadas` (`tipo`, `entidadId`)
                    """.trimIndent()
                )
            }
        }

        // Migración real 3 → 4: la tabla alertas_enviadas pasa de una clave
        // única (tipo, entidadId) a (tipo, entidadId, fechaAlerta), para que
        // una vacuna reprogramada (o una nueva estimación de parto) SÍ pueda
        // generar una alerta nueva en vez de quedar bloqueada por la alerta
        // de la fecha anterior. No se pierde el historial: se agrega la
        // columna con un valor por defecto y se reconstruye el índice.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `alertas_enviadas` ADD COLUMN `fechaAlerta` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "DROP INDEX IF EXISTS `index_alertas_enviadas_tipo_entidadId`"
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_alertas_enviadas_tipo_entidadId_fechaAlerta`
                    ON `alertas_enviadas` (`tipo`, `entidadId`, `fechaAlerta`)
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): GanaderaDatabase {
            return INSTANCE ?: synchronized(this) {
                // Solo migraciones reales, nunca fallbackToDestructiveMigration():
                // cada salto de versión (2→3, 3→4) tiene su propia Migration que
                // conserva los datos existentes. Si en el futuro se agrega una
                // versión nueva, se debe agregar aquí su propia Migration real,
                // nunca borrar la base como "solución".
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GanaderaDatabase::class.java,
                    "ganadopro_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}