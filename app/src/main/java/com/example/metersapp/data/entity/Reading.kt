package com.example.metersapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "readings",
    foreignKeys = [
        ForeignKey(
            entity = Meter::class,
            parentColumns = ["id"],
            childColumns = ["meterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("meterId")]
)
data class Reading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val meterId: Long,

    val date: Long,

    val value: Double? = null,

    val valueDay: Double? = null,

    val valueNight: Double? = null
)