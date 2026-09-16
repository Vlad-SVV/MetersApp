package com.example.metersapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.metersapp.data.dao.MeterDao
import com.example.metersapp.data.dao.MeterObjectDao
import com.example.metersapp.data.entity.Meter
import com.example.metersapp.data.entity.MeterObject

@Database(
    entities = [MeterObject::class, Meter::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun meterObjectDao(): MeterObjectDao
    abstract fun meterDao(): MeterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Получить единственный экземпляр базы данных */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meters.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}