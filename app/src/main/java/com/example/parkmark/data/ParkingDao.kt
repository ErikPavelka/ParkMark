package com.example.parkmark.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingDao {
    //vlozi record do databazy
    @Insert
    suspend fun insertParkingRecord(record: ParkingRecord)

    //nacita celu historiu
    @Query("SELECT * FROM parking_records ORDER BY startTime DESC")
    fun getAllParkingRecords(): Flow<List<ParkingRecord>>

    //vymaze historiu recordov
    @Query("DELETE FROM parking_records")
    suspend fun deleteAllParkingRecords()

    @Delete
    suspend fun deleteParkingRecord(record : ParkingRecord)


}