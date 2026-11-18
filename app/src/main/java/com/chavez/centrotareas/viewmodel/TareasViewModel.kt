package com.chavez.centrotareas.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chavez.centrotareas.data.HistorialRepository
import com.chavez.centrotareas.data.TareasRepository
import com.chavez.centrotareas.model.CargaUi
import com.chavez.centrotareas.model.ModoCarga
import com.chavez.centrotareas.model.PrimosUi
import com.chavez.centrotareas.model.TiemposCarga
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.IOException

class TareasViewModel(
    private val repo: TareasRepository,
    private val historialRepo: HistorialRepository
) : ViewModel() {

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
            _primos.value = PrimosUi.Calculando(0f)
            val inicio = System.currentTimeMillis()
            try {
                val cantidad = repo.contarPrimos(hasta) { progreso ->
                    _primos.value = PrimosUi.Calculando(progreso)
                }
                val milisegundos = System.currentTimeMillis() - inicio
                _primos.value = PrimosUi.Listo(cantidad, milisegundos)
                registrarHistorial("Primos hasta $hasta: $cantidad ($milisegundos ms)")
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

    private val _tiempos = MutableStateFlow(TiemposCarga())
    val tiempos: StateFlow<TiemposCarga> = _tiempos.asStateFlow()

    val historial: StateFlow<List<String>> = historialRepo.historial
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun borrarHistorial() {
        viewModelScope.launch {
            try {
                historialRepo.borrar()
            } catch (e: IOException) {
                Log.w(TAG_HISTORIAL, "No se pudo borrar el historial", e)
            }
        }
    }

    fun cargarUnaPorUna() = cargar(ModoCarga.UNA_POR_UNA) {
        val perfil = repo.cargarPerfil()
        val pedidos = repo.cargarPedidos(_simularError.value, _redLenta.value)
        perfil to pedidos
    }

    fun cargarALaVez() = cargar(ModoCarga.A_LA_VEZ) {
        coroutineScope {
            val perfilDiferido = async { repo.cargarPerfil() }
            val pedidosDiferidos = async { repo.cargarPedidos(_simularError.value, _redLenta.value) }
            perfilDiferido.await() to pedidosDiferidos.await()
        }
    }

    fun reintentar() = cargarALaVez()

    private fun cargar(modo: ModoCarga, obtener: suspend () -> Pair<String, List<String>>) {
        if (_carga.value is CargaUi.Cargando || _carga.value is CargaUi.Reintentando) return

        viewModelScope.launch {
            _carga.value = CargaUi.Cargando
            val inicio = System.currentTimeMillis()
            try {
                val (perfil, pedidos) = ejecutarConReintento { obtener() }
                val milisegundos = System.currentTimeMillis() - inicio

                _tiempos.update { actual ->
                    when (modo) {
                        ModoCarga.UNA_POR_UNA -> actual.copy(unaPorUna = milisegundos)
                        ModoCarga.A_LA_VEZ -> actual.copy(aLaVez = milisegundos)
                    }
                }

                _carga.value = CargaUi.Exito(
                    modo = modo,
                    perfil = perfil,
                    pedidos = pedidos.size,
                    milisegundos = milisegundos
                )
                registrarHistorial("${modo.etiqueta}: $milisegundos ms")
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

    private fun registrarHistorial(entrada: String) {
        viewModelScope.launch {
            try {
                historialRepo.agregar(entrada)
            } catch (e: IOException) {
                Log.w(TAG_HISTORIAL, "No se pudo guardar en el historial", e)
            }
        }
    }

    private companion object {
        const val LIMITE_CARGA_MS = 6000L
        const val INTENTOS_MAXIMOS = 3
        const val TAG_HISTORIAL = "HISTORIAL"
    }
}