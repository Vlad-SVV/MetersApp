package com.example.metersapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.metersapp.data.dao.MeterDao
import com.example.metersapp.data.dao.MeterObjectDao
import com.example.metersapp.data.dao.ReadingDao
import com.example.metersapp.data.entity.Meter
import com.example.metersapp.data.entity.MeterObject
import com.example.metersapp.data.entity.Reading

@Database(
    entities = [MeterObject::class, Meter::class, Reading::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun meterObjectDao(): MeterObjectDao
    abstract fun meterDao(): MeterDao
    abstract fun readingDao(): ReadingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meters ADD COLUMN tariff REAL")
                db.execSQL("ALTER TABLE meters ADD COLUMN tariffNight REAL")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meters.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}