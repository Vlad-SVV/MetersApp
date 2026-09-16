package com.example.metersapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Тип счётчика.
 */
enum class MeterType {
    COLD_WATER,   // Холодная вода
    HOT_WATER,    // Горячая вода
    ELECTRICITY   // Электроэнергия
}

/**
 * Счётчик (воды или электроэнергии).
 */
@Entity(
    tableName = "meters",
    foreignKeys = [
        ForeignKey(
            entity = MeterObject::class,
            parentColumns = ["id"],
            childColumns = ["objectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("objectId")]
)
data class Meter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** К какому объекту (квартира/дом) относится счётчик */
    val objectId: Long,

    /** Тип счётчика: ХВС, ГВС или электро */
    val type: MeterType,

    /** Наименование, например «Кухня» или «Ванная» */
    val name: String,

    /** Заводской номер */
    val serialNumber: String,

    /** Дата поверки (timestamp в миллисекундах) */
    val verificationDate: Long,

    /** Дата окончания поверки (timestamp в миллисекундах) */
    val verificationEndDate: Long,

    /** Разрядность показаний (по умолчанию 6) */
    val digits: Int = 6,

    /** Двухтарифный ли счётчик (только для электро) */
    val isTwoTariff: Boolean = false
)