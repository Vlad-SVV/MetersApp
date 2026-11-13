package com.example.metersapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.metersapp.data.entity.Reading
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {

    /** Все показания для счётчика, отсортированные по дате (новые сверху) */
    @Query("SELECT * FROM readings WHERE meterId = :meterId ORDER BY date DESC")
    fun getByMeter(meterId: Long): Flow<List<Reading>>

    /** Последнее показание счётчика (для расчёта расхода) */
    @Query("SELECT * FROM readings WHERE meterId = :meterId ORDER BY date DESC LIMIT 1")
    suspend fun getLastReading(meterId: Long): Reading?

    /** Добавить показание */
    @Insert
    suspend fun insert(reading: Reading): Long

    /** Обновить показание */
    @Update
    suspend fun update(reading: Reading)

    /** Удалить показание */
    @Delete
    suspend fun delete(reading: Reading)
}