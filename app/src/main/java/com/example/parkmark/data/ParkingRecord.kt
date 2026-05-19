package com.example.parkmark.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parking_records")
data class ParkingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val latitude: Double,
    val longtitude: Double,
    val address: String,
    val startTime: Long,
    val expireTime: Long?,
    val note: String
)
