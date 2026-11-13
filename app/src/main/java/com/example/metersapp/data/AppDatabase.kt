package com.example.metersapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.metersapp.data.dao.MeterDao
import com.example.metersapp.data.dao.MeterObjectDao
import com.example.metersapp.data.dao.ReadingDao
import com.example.metersapp.data.entity.Meter
import com.example.metersapp.data.entity.MeterObject
import com.example.metersapp.data.entity.Reading

@Database(
    entities = [MeterObject::class, Meter::class, Reading::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun meterObjectDao(): MeterObjectDao
    abstract fun meterDao(): MeterDao
    abstract fun readingDao(): ReadingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meters.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true) // ⚠️ на этапе разработки данные будут удаляться при обновлении схемы
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}