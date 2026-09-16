package com.example.metersapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.metersapp.data.AppDatabase
import com.example.metersapp.data.entity.MeterObject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val objectDao = db.meterObjectDao()

    val objects: StateFlow<List<MeterObject>> = objectDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addObject(name: String) {
        viewModelScope.launch {
            objectDao.insert(MeterObject(name = name))
        }
    }
}