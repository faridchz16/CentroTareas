package com.chavez.centrotareas.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chavez.centrotareas.data.TareasRepository
import com.chavez.centrotareas.model.CargaUi
import com.chavez.centrotareas.model.PrimosUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.IOException

class TareasViewModel(private val repo: TareasRepository) : ViewModel() {

    private val _mensaje = MutableStateFlow("Listo")
    val mensaje: StateFlow<String> = _mensaje.asStateFlow()

    private val _progreso = MutableStateFlow(0f)
    val progreso: StateFlow<Float> = _progreso.asStateFlow()

    private val _corriendo = MutableStateFlow(false)
    val corriendo: StateFlow<Boolean> = _corriendo.asStateFlow()

    private var trabajo: Job? = null

    fun tareaLarga() {
        if (_corriendo.value) return

        trabajo = viewModelScope.launch {
            _corriendo.value = true
            try {
                _progreso.value = 0f
                _mensaje.value = "Trabajando..."
                Log.d("HILO", "Antes: ${Thread.currentThread().name}")

                repo.progresoTarea().collect { valor ->
                    _progreso.value = valor
                }

                Log.d("HILO", "Despues: ${Thread.currentThread().name}")
                _mensaje.value = "Terminó"
            } finally {
                _corriendo.value = false
            }
        }
    }

    fun cancelar() {
        trabajo?.cancel()
        trabajo = null
        _mensaje.value = "Cancelado"
    }

    private val _primos = MutableStateFlow<PrimosUi>(PrimosUi.Inicial)
    val primos: StateFlow<PrimosUi> = _primos.asStateFlow()

    private var trabajoPrimos: Job? = null

    fun contar(hasta: Int) {
        if (_primos.value is PrimosUi.Calculando) return

        trabajoPrimos = viewModelScope.launch {
            _primos.value = PrimosUi.Calculando
            val inicio = System.currentTimeMillis()
            try {
                val cantidad = repo.contarPrimos(hasta)
                _primos.value = PrimosUi.Listo(cantidad, System.currentTimeMillis() - inicio)
            } catch (e: CancellationException) {
                _primos.value = PrimosUi.Cancelado
                throw e
            }
        }
    }

    fun cancelarPrimos() {
        trabajoPrimos?.cancel()
        trabajoPrimos = null
    }

    private val _carga = MutableStateFlow<CargaUi>(CargaUi.Inicial)
    val carga: StateFlow<CargaUi> = _carga.asStateFlow()

    private val _simularError = MutableStateFlow(false)
    val simularError: StateFlow<Boolean> = _simularError.asStateFlow()

    fun cambiarSimularError(valor: Boolean) {
        _simularError.value = valor
    }

    private val _redLenta = MutableStateFlow(false)
    val redLenta: StateFlow<Boolean> = _redLenta.asStateFlow()

    fun cambiarRedLenta(valor: Boolean) {
        _redLenta.value = valor
    }

    fun cargarUnaPorUna() = cargar("Una por una") {
        val perfil = repo.cargarPerfil()
        val pedidos = repo.cargarPedidos(_simularError.value, _redLenta.value)
        perfil to pedidos
    }

    fun cargarALaVez() = cargar("A la vez") {
        coroutineScope {
            val perfilDiferido = async { repo.cargarPerfil() }
            val pedidosDiferidos = async { repo.cargarPedidos(_simularError.value, _redLenta.value) }
            perfilDiferido.await() to pedidosDiferidos.await()
        }
    }

    fun reintentar() = cargarALaVez()

    private fun cargar(modo: String, obtener: suspend () -> Pair<String, List<String>>) {
        if (_carga.value is CargaUi.Cargando || _carga.value is CargaUi.Reintentando) return

        viewModelScope.launch {
            _carga.value = CargaUi.Cargando
            val inicio = System.currentTimeMillis()
            try {
                val (perfil, pedidos) = ejecutarConReintento { obtener() }
                _carga.value = CargaUi.Exito(
                    modo = modo,
                    perfil = perfil,
                    pedidos = pedidos.size,
                    milisegundos = System.currentTimeMillis() - inicio
                )
            } catch (e: TimeoutCancellationException) {
                _carga.value = CargaUi.Error("Tardó demasiado (más de ${LIMITE_CARGA_MS / 1000} s)")
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _carga.value = CargaUi.Error("No se pudo cargar tras $INTENTOS_MAXIMOS intentos: ${e.message}")
            } catch (e: Exception) {
                _carga.value = CargaUi.Error("No se pudo cargar: ${e.message}")
            }
        }
    }

    private suspend fun <T> ejecutarConReintento(bloque: suspend () -> T): T {
        repeat(INTENTOS_MAXIMOS - 1) { indice ->
            try {
                return withTimeout(LIMITE_CARGA_MS) { bloque() }
            } catch (e: IOException) {
                _carga.value = CargaUi.Reintentando(indice + 2, INTENTOS_MAXIMOS)
                delay(1000L * (indice + 1))
            }
        }
        return withTimeout(LIMITE_CARGA_MS) { bloque() }
    }

    private companion object {
        const val LIMITE_CARGA_MS = 6000L
        const val INTENTOS_MAXIMOS = 3
    }
}