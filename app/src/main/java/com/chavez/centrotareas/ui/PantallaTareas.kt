package com.chavez.centrotareas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chavez.centrotareas.model.CargaUi
import com.chavez.centrotareas.model.PrimosUi
import com.chavez.centrotareas.ui.theme.ExitoContenedor
import com.chavez.centrotareas.ui.theme.ExitoContenedorOscuro
import com.chavez.centrotareas.ui.theme.SobreExitoContenedor
import com.chavez.centrotareas.ui.theme.SobreExitoContenedorOscuro
import com.chavez.centrotareas.viewmodel.TareasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaTareas(vm: TareasViewModel) {
    val mensaje by vm.mensaje.collectAsState()
    val progreso by vm.progreso.collectAsState()
    val corriendo by vm.corriendo.collectAsState()
    val primos by vm.primos.collectAsState()
    val carga by vm.carga.collectAsState()
    val simularError by vm.simularError.collectAsState()
    val redLenta by vm.redLenta.collectAsState()

    var toques by rememberSaveable { mutableIntStateOf(0) }
    var textoPrimos by rememberSaveable { mutableStateOf("5000000") }
    var hastaCalculado by rememberSaveable { mutableIntStateOf(0) }

    val hasta = textoPrimos.toIntOrNull()
    val numeroValido = hasta != null && hasta >= 2
    val calculando = primos is PrimosUi.Calculando
    val cargando = carga is CargaUi.Cargando

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CentroTareas", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TarjetaTarea(titulo = "Tarea con progreso") {
                Button(onClick = { toques++ }) {
                    Text("Tócame: $toques")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.tareaLarga() },
                        enabled = !corriendo
                    ) {
                        Text("Iniciar")
                    }
                    OutlinedButton(
                        onClick = { vm.cancelar() },
                        enabled = corriendo
                    ) {
                        Text("Cancelar")
                    }
                }

                LinearProgressIndicator(
                    progress = { progreso },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "${(progreso * 100).toInt()} %",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = mensaje,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TarjetaTarea(titulo = "Contar números primos") {
                OutlinedTextField(
                    value = textoPrimos,
                    onValueChange = { nuevo ->
                        if (nuevo.all { it.isDigit() }) textoPrimos = nuevo
                    },
                    label = { Text("Hasta el número") },
                    singleLine = true,
                    enabled = !calculando,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            hasta?.let {
                                hastaCalculado = it
                                vm.contar(it)
                            }
                        },
                        enabled = numeroValido && !calculando
                    ) {
                        Text("Contar")
                    }
                    OutlinedButton(
                        onClick = { vm.cancelarPrimos() },
                        enabled = calculando
                    ) {
                        Text("Cancelar")
                    }
                }

                when (val estadoPrimos = primos) {
                    PrimosUi.Inicial -> Unit
                    PrimosUi.Calculando -> Cargando(texto = "Calculando...")
                    is PrimosUi.Listo -> RecuadroExito(
                        lineas = listOf(
                            "Primos hasta $hastaCalculado: ${estadoPrimos.cantidad}",
                            "Tiempo: ${estadoPrimos.milisegundos} ms"
                        )
                    )
                    PrimosUi.Cancelado -> Text(
                        text = "Cancelado",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TarjetaTarea(titulo = "Dos cargas") {
                Row(
                    modifier = Modifier.align(Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = simularError,
                        onCheckedChange = { vm.cambiarSimularError(it) },
                        enabled = !cargando
                    )
                    Text(
                        text = "Simular error",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = redLenta,
                        onCheckedChange = { vm.cambiarRedLenta(it) },
                        enabled = !cargando
                    )
                    Text(
                        text = "Red lenta",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { vm.cargarUnaPorUna() },
                        enabled = !cargando
                    ) {
                        Text("Una por una")
                    }
                    Button(
                        onClick = { vm.cargarALaVez() },
                        enabled = !cargando
                    ) {
                        Text("A la vez")
                    }
                }

                when (val estadoCarga = carga) {
                    CargaUi.Inicial -> Text(
                        text = "«Una por una» tarda ≈ 5000 ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CargaUi.Cargando -> Cargando(texto = "Cargando...")
                    is CargaUi.Exito -> RecuadroExito(
                        lineas = listOf(
                            "Modo: ${estadoCarga.modo}",
                            "Perfil: ${estadoCarga.perfil}",
                            "Pedidos: ${estadoCarga.pedidos}",
                            "Tiempo: ${estadoCarga.milisegundos} ms"
                        )
                    )
                    is CargaUi.Error -> {
                        RecuadroError(mensaje = estadoCarga.mensaje)
                        OutlinedButton(onClick = { vm.reintentar() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaTarea(
    titulo: String,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )
            contenido()
        }
    }
}

@Composable
private fun Cargando(texto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
        )
        Text(
            text = texto,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecuadroExito(lineas: List<String>) {
    val oscuro = isSystemInDarkTheme()
    val fondo = if (oscuro) ExitoContenedorOscuro else ExitoContenedor
    val colorTexto = if (oscuro) SobreExitoContenedorOscuro else SobreExitoContenedor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(fondo)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        lineas.forEachIndexed { indice, linea ->
            Text(
                text = linea,
                color = colorTexto,
                fontWeight = if (indice == 0) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RecuadroError(mensaje: String) {
    Text(
        text = mensaje,
        color = MaterialTheme.colorScheme.onErrorContainer,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp)
    )
}