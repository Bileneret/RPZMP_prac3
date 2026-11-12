package ua.kpi.iot_lighting.prac3.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Єдина база кешу застосунку (версія 1).
 *
 * Примірник створюється один раз в [ua.kpi.iot_lighting.prac3.di.AppContainer]
 * і передається у Data Source — типова схема для ручного DI без Hilt.
 */
@Database(entities = [LightDeviceEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lightDeviceDao(): LightDeviceDao

    companion object {
        /** Імʼя файлу бази в внутрішньому сховищі застосунку. */
        private const val DB_NAME = "iot_lighting_prac3.db"

        /** Побудова singleton-бази з `fallbackToDestructiveMigration` відсутній:
         *  версія фінальна, міграцій не передбачено — зміни схеми означатимуть v2. */
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME).build()
    }
}
