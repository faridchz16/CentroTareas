package com.chavez.centrotareas.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "historial")

class HistorialRepositoryDataStore(private val context: Context) : HistorialRepository {

    override val historial: Flow<List<String>> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferencias -> preferencias.entradas() }

    override suspend fun agregar(entrada: String) {
        context.dataStore.edit { preferencias ->
            val nuevas = (listOf(entrada) + preferencias.entradas()).take(MAXIMO_ENTRADAS)
            preferencias[CLAVE_ENTRADAS] = nuevas.joinToString(SEPARADOR)
        }
    }

    override suspend fun borrar() {
        context.dataStore.edit { preferencias ->
            preferencias.remove(CLAVE_ENTRADAS)
        }
    }

    private fun Preferences.entradas(): List<String> =
        this[CLAVE_ENTRADAS]?.takeIf { it.isNotEmpty() }?.split(SEPARADOR) ?: emptyList()

    private companion object {
        val CLAVE_ENTRADAS = stringPreferencesKey("entradas")
        const val SEPARADOR = "\n"
        const val MAXIMO_ENTRADAS = 5
    }
}