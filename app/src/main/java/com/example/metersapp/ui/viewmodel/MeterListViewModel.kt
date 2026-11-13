package com.example.metersapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.metersapp.data.AppDatabase
import com.example.metersapp.data.entity.Meter
import com.example.metersapp.data.entity.MeterType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MeterListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val meterDao = db.meterDao()

    private val objectIdFlow = MutableStateFlow<Long?>(null)

    val meters: StateFlow<List<Meter>> = objectIdFlow
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else meterDao.getByObject(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setObjectId(id: Long) {
        objectIdFlow.value = id
    }

    fun addMeter(
        objectId: Long,
        type: MeterType,
        name: String,
        serialNumber: String,
        verificationDate: Long,
        verificationEndDate: Long,
        isTwoTariff: Boolean = false,
        tariff: Double? = null,
        tariffNight: Double? = null
    ) {
        viewModelScope.launch {
            meterDao.insert(
                Meter(
                    objectId = objectId,
                    type = type,
                    name = name,
                    serialNumber = serialNumber,
                    verificationDate = verificationDate,
                    verificationEndDate = verificationEndDate,
                    isTwoTariff = isTwoTariff,
                    tariff = tariff,
                    tariffNight = tariffNight
                )
            )
        }
    }
}