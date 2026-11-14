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

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

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
    // Для показа сообщений об успехе или ошибке
    private val _importStatus = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _importStatus

    fun importReadings(uri: Uri) {
        val meterId = meterIdFlow.value ?: return
        viewModelScope.launch {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = inputStream.bufferedReader()
                    var line = reader.readLine() // Пропускаем строку заголовков
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    var count = 0

                    while (reader.readLine().also { line = it } != null) {
                        if (line.isBlank()) continue

                        // Разделяем строку по точке с запятой (как мы сохраняли)
                        val parts = line.split(";")
                        if (parts.size >= 4) {
                            val dateStr = parts[0]
                            val valStr = parts[1].replace(',', '.')
                            val dayStr = parts[2].replace(',', '.')
                            val nightStr = parts[3].replace(',', '.')

                            val date = dateFormat.parse(dateStr)?.time ?: continue
                            val value = valStr.toDoubleOrNull()
                            val day = dayStr.toDoubleOrNull()
                            val night = nightStr.toDoubleOrNull()

                            // Если есть хоть одно значение — сохраняем
                            if (value != null || (day != null && night != null)) {
                                readingDao.insert(
                                    Reading(
                                        meterId = meterId,
                                        date = date,
                                        value = value,
                                        valueDay = day,
                                        valueNight = night
                                    )
                                )
                                count++
                            }
                        }
                    }
                    _importStatus.value = "Успешно импортировано записей: $count"
                }
            } catch (e: Exception) {
                _importStatus.value = "Ошибка импорта: ${e.message}"
            }
        }
    }

    fun clearImportStatus() {
        _importStatus.value = null
    }
}