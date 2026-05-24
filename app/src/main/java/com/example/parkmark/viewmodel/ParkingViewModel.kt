package com.example.parkmark.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.parkmark.data.ParkingDao
import com.example.parkmark.data.ParkingRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.time.Duration.Companion.days

class ParkingViewModel(private val dao: ParkingDao) : ViewModel() {


    val parkingHistory : StateFlow<List<ParkingRecord>> = dao.getAllParkingRecords().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveParking(latitude: Double, longitude: Double, address: String, note: String, expireTime: Long?) {
        viewModelScope.launch {
            val newRecord = ParkingRecord(latitude = latitude, longitude = longitude, address = address, note = note, startTime = System.currentTimeMillis(), expireTime = expireTime)

            dao.insertParkingRecord(newRecord)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.deleteAllParkingRecords()
        }
    }

    fun deleteParking(parking : ParkingRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteParkingRecord(parking)
        }
    }
    fun deleteOldHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val thresholdTime = System.currentTimeMillis() - 30.days.inWholeMilliseconds
            dao.deleteRecordsOlder(thresholdTime)
        }
    }
}

class ParkingViewModelFactory(private val dao: ParkingDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParkingViewModel::class.java)) {
            return ParkingViewModel(dao) as T
        }
        throw IllegalArgumentException("neznama trieda ViewModelu")
    }
}