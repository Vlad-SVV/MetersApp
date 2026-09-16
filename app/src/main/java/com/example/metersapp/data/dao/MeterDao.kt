package com.example.metersapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.metersapp.data.entity.Meter
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterDao {

    /** Все счётчики (для главного экрана) */
    @Query("SELECT * FROM meters ORDER BY name")
    fun getAll(): Flow<List<Meter>>

    /** Счётчики конкретного объекта */
    @Query("SELECT * FROM meters WHERE objectId = :objectId ORDER BY name")
    fun getByObject(objectId: Long): Flow<List<Meter>>

    /** Один счётчик по id */
    @Query("SELECT * FROM meters WHERE id = :id")
    suspend fun getById(id: Long): Meter?

    /** Добавить счётчик */
    @Insert
    suspend fun insert(meter: Meter): Long

    /** Обновить */
    @Update
    suspend fun update(meter: Meter)

    /** Удалить */
    @Delete
    suspend fun delete(meter: Meter)
}