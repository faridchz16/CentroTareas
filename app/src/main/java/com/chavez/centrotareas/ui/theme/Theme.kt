package com.chavez.centrotareas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EsquemaClaro = lightColorScheme(
    primary = IndigoPrimario,
    onPrimary = IndigoSobrePrimario,
    primaryContainer = IndigoContenedor,
    onPrimaryContainer = IndigoSobreContenedor,
    secondary = CoralSecundario,
    onSecondary = CoralSobreSecundario,
    secondaryContainer = CoralContenedor,
    onSecondaryContainer = CoralSobreContenedor,
    background = FondoClaro,
    onBackground = TextoOscuro,
    surface = SuperficieClara,
    onSurface = TextoOscuro,
    surfaceVariant = SuperficieVariante,
    onSurfaceVariant = TextoSecundario,
    outline = Contorno,
    outlineVariant = ContornoSuave,
    error = ErrorRojo,
    errorContainer = ErrorContenedor,
    onErrorContainer = SobreErrorContenedor
)

private val EsquemaOscuro = darkColorScheme(
    primary = IndigoPrimarioOscuro,
    onPrimary = IndigoSobrePrimarioOscuro,
    primaryContainer = IndigoContenedorOscuro,
    onPrimaryContainer = IndigoSobreContenedorOscuro,
    secondary = CoralSecundarioOscuro,
    onSecondary = CoralSobreSecundarioOscuro,
    secondaryContainer = CoralContenedorOscuro,
    onSecondaryContainer = CoralContenedor,
    background = FondoOscuro,
    onBackground = TextoClaro,
    surface = SuperficieOscura,
    onSurface = TextoClaro,
    surfaceVariant = SuperficieVarianteOscura,
    onSurfaceVariant = TextoSecundarioOscuro,
    outline = ContornoOscuro,
    outlineVariant = ContornoSuaveOscuro,
    error = ErrorRojoOscuro,
    errorContainer = ErrorContenedorOscuro,
    onErrorContainer = ErrorContenedor
)

@Composable
fun CentroTareasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Typography,
        content = content
    )
}