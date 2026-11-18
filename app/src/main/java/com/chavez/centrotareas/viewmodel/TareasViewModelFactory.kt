package com.chavez.centrotareas.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chavez.centrotareas.data.HistorialRepositoryDataStore
import com.chavez.centrotareas.data.TareasRepositoryLocal

class TareasViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TareasViewModel::class.java)) {
            return TareasViewModel(
                repo = TareasRepositoryLocal(),
                historialRepo = HistorialRepositoryDataStore(context)
            ) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}