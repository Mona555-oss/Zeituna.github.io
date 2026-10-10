package com.alfleyla.zeituna.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val LightColorPalette = lightColors(
    primary = TurquoisePrimary,
    primaryVariant = TurquoiseDark,
    secondary = TurquoiseBackground,
    background = TurquoiseLight,
    surface = White,
    onPrimary = White,
    onSecondary = Black,
    onBackground = Black,
    onSurface = Black,
)

@Composable
fun ZeitunaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = LightColorPalette,
        content = content
    )
}
