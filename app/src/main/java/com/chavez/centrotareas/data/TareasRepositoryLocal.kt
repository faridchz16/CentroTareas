package com.chavez.centrotareas.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException

class TareasRepositoryLocal : TareasRepository {

    override fun progresoTarea(): Flow<Float> = flow {
        Log.d("HILO", "Dentro: ${Thread.currentThread().name}")
        for (i in 1..100) {
            delay(40)
            emit(i / 100f)
        }
    }.flowOn(Dispatchers.Default)

    override suspend fun cargarPerfil(): String {
        delay(2000)
        return "Ana Torres (ficticia)"
    }

    override suspend fun cargarPedidos(simularError: Boolean, lenta: Boolean): List<String> {
        delay(if (lenta) 8000 else 3000)
        if (simularError) throw IOException("Sin conexión")
        return listOf("Pedido 101", "Pedido 102", "Pedido 103")
    }

    override suspend fun contarPrimos(hasta: Int, alAvanzar: (Float) -> Unit): Int = withContext(Dispatchers.Default) {
        var cantidad = 0
        var ultimoPorcentaje = -1
        for (n in 2..hasta) {
            ensureActive()
            if (esPrimo(n)) cantidad++

            val porcentaje = ((n.toLong() * 100) / hasta).toInt()
            if (porcentaje != ultimoPorcentaje) {
                ultimoPorcentaje = porcentaje
                alAvanzar(porcentaje / 100f)
            }
        }
        cantidad
    }

    private fun esPrimo(n: Int): Boolean {
        var d = 2
        while (d.toLong() * d <= n) {
            if (n % d == 0) return false
            d++
        }
        return true
    }
}