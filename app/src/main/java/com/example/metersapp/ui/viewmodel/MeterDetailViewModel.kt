package com.example.metersapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.metersapp.data.AppDatabase
import com.example.metersapp.data.entity.Meter
import com.example.metersapp.data.entity.Reading
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MeterDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val meterDao = db.meterDao()
    private val readingDao = db.readingDao()

    private val meterIdFlow = MutableStateFlow<Long?>(null)

    val meter: StateFlow<Meter?> = meterIdFlow
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else meterDao.observeById(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val readings: StateFlow<List<Reading>> = meterIdFlow
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else readingDao.getByMeter(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setMeterId(id: Long) {
        meterIdFlow.value = id
    }

    fun updateReading(reading: Reading) {
        viewModelScope.launch {
            readingDao.update(reading)
        }
    }

    fun deleteReading(reading: Reading) {
        viewModelScope.launch {
            readingDao.delete(reading)
        }
    }

    fun updateMeter(meter: Meter) {
        viewModelScope.launch {
            meterDao.update(meter)
        }
    }

    fun addReading(date: Long, value: Double?, valueDay: Double?, valueNight: Double?) {
        val meterId = meterIdFlow.value ?: return
        viewModelScope.launch {
            readingDao.insert(
                Reading(
                    meterId = meterId,
                    date = date,
                    value = value,
                    valueDay = valueDay,
                    valueNight = valueNight
                )
            )
        }
    }
}