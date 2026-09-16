package com.example.metersapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Объект учёта — квартира или дом.
 */
@Entity(tableName = "objects")
data class MeterObject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Название объекта, например «Квартира» или «Дом» */
    val name: String
)