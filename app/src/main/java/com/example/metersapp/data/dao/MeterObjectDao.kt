package com.example.metersapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.metersapp.data.entity.MeterObject
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterObjectDao {

    /** Получить все объекты (квартиры, дома). Flow — «живой» список, обновляется сам. */
    @Query("SELECT * FROM objects ORDER BY name")
    fun getAll(): Flow<List<MeterObject>>

    /** Добавить объект */
    @Insert
    suspend fun insert(obj: MeterObject): Long

    /** Обновить объект */
    @Update
    suspend fun update(obj: MeterObject)

    /** Удалить объект (вместе со всеми его счётчиками — из-за CASCADE) */
    @Delete
    suspend fun delete(obj: MeterObject)
}