package com.fanlens.ai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FanLens keeps its cricket identity (red ball on a dark pitch) and adopts the
 * structural rules from DESIGN.md: flat surfaces instead of shadows, hairline
 * rules for containment, pill CTAs, restrained accent discipline, and hierarchy
 * from size/spacing rather than heavy bold weights.
 */
val BallRed = Color(0xFFE53935)
val PitchDark = Color(0xFF0B1D26)
val PitchMid = Color(0xFF12303C)
val Chalk = Color(0xFFF5F7F7)
val SelfGold = Color(0xFFFFC24B)

// DESIGN.md neutrals, adapted to the dark pitch surface.
val Hairline = Color(0x1FFFFFFF) // 1px translucent rule, the containment default
val RuleStrong = Color(0x33FFFFFF)
val MutedSlate = Color(0xFF93939F)
val LabelSlate = Color(0xFF75758A)
val InkOnDark = Color(0xFFEDF0F2)

val FanLensColorScheme = darkColorScheme(
    primary = BallRed,
    onPrimary = Color.White,
    background = PitchDark,
    onBackground = Chalk,
    surface = PitchMid,
    onSurface = Chalk,
    secondary = SelfGold,
    onSecondary = PitchDark,
    outline = Hairline,
    outlineVariant = RuleStrong,
)

// DESIGN.md radius scale: 4 / 8 / 16 / 22 / 32 pill.
val FanLensShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Pill CTA (DESIGN.md `button-primary`: 32px radius). */
val PillShape = RoundedCornerShape(32.dp)

/** Tight display type; size and spacing carry hierarchy, not weight. */
val DisplayStyle = TextStyle(
    fontSize = 28.sp,
    lineHeight = 32.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = (-0.56).sp,
)

/** Uppercase technical marker (DESIGN.md `mono-label`, adapted). */
val LabelStyle = TextStyle(
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.28.sp,
)

/** De-emphasized metadata. */
val MetaStyle = TextStyle(
    fontSize = 14.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
)

@Composable
fun FanLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FanLensColorScheme,
        shapes = FanLensShapes,
        content = content,
    )
}