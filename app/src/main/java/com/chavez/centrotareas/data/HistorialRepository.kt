package com.chavez.centrotareas.data

import kotlinx.coroutines.flow.Flow

interface HistorialRepository {
    val historial: Flow<List<String>>
    suspend fun agregar(entrada: String)
    suspend fun borrar()
}