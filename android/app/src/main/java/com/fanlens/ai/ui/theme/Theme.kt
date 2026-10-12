package com.fanlens.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BallRed = Color(0xFFE53935)
val PitchDark = Color(0xFF0B1D26)
val PitchMid = Color(0xFF12303C)
val Chalk = Color(0xFFF5F7F7)
val SelfGold = Color(0xFFFFC24B)

val FanLensColorScheme = darkColorScheme(
    primary = BallRed,
    onPrimary = Color.White,
    background = PitchDark,
    onBackground = Chalk,
    surface = PitchMid,
    onSurface = Chalk,
    secondary = SelfGold,
    onSecondary = PitchDark,
)

@Composable
fun FanLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FanLensColorScheme, content = content)
}
