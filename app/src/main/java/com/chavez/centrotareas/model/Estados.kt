package com.chavez.centrotareas.model

sealed interface PrimosUi {
    data object Inicial : PrimosUi
    data class Calculando(val progreso: Float) : PrimosUi
    data class Listo(val cantidad: Int, val milisegundos: Long) : PrimosUi
    data object Cancelado : PrimosUi
}

enum class ModoCarga(val etiqueta: String) {
    UNA_POR_UNA("Una por una"),
    A_LA_VEZ("A la vez")
}

data class TiemposCarga(
    val unaPorUna: Long? = null,
    val aLaVez: Long? = null
) {
    val tieneDatos: Boolean
        get() = unaPorUna != null || aLaVez != null

    val ahorroMs: Long?
        get() = if (unaPorUna != null && aLaVez != null) unaPorUna - aLaVez else null

    val ahorroPorcentaje: Int?
        get() {
            val ahorro = ahorroMs ?: return null
            val base = unaPorUna ?: return null
            return if (base > 0) ((ahorro * 100) / base).toInt() else null
        }
}

sealed interface CargaUi {
    data object Inicial : CargaUi
    data object Cargando : CargaUi
    data class Reintentando(val intento: Int, val total: Int) : CargaUi
    data class Exito(
        val modo: ModoCarga,
        val perfil: String,
        val pedidos: Int,
        val milisegundos: Long
    ) : CargaUi
    data class Error(val mensaje: String) : CargaUi
}