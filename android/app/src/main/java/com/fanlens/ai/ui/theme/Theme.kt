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

// DESIGN.md & HTML companion neutrals
val Hairline = Color(0xFFE2E2E8)
val RuleStrong = Color(0xFFC8C5CB)
val MutedSlate = Color(0xFF75758A)
val LabelSlate = Color(0xFF47464B)
val InkOnDark = Color(0xFF1A1C1C)

// Modern Light Companion Theme Colors (HTML spec)
val SurfaceBg = Color(0xFFF9F9F9)
val SurfaceCard = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF3F3F4)
val SurfaceContainer = Color(0xFFEEEEEE)
val SurfaceContainerHigh = Color(0xFFE8E8E8)
val SurfaceContainerHighest = Color(0xFFE2E2E2)
val OutlineVariantColor = Color(0xFFC8C5CB)
val OutlineColor = Color(0xFF78767B)
val PrimaryBlack = Color(0xFF000000)
val PrimaryContainerColor = Color(0xFF1B1B20)
val SecondaryBlue = Color(0xFF0055C7)
val SecondaryContainerColor = Color(0xFF2C6EE8)
val SecondaryFixedColor = Color(0xFFD9E2FF)
val OnSecondaryFixedColor = Color(0xFF001945)
val ErrorRed = Color(0xFFBA1A1A)
val ErrorContainerColor = Color(0xFFFFDAD6)
val EmeraldLive = Color(0xFF059669)
val EmeraldLiveBg = Color(0xFFD1FAE5)
val AmberTrophy = Color(0xFFF59E0B)
val AmberTrophyBg = Color(0xFFFEF3C7)

val FanLensColorScheme = androidx.compose.material3.lightColorScheme(
    primary = PrimaryBlack,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerColor,
    onPrimaryContainer = Color(0xFF848389),
    secondary = SecondaryBlue,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainerColor,
    onSecondaryContainer = Color.White,
    background = SurfaceBg,
    onBackground = InkOnDark,
    surface = SurfaceCard,
    onSurface = InkOnDark,
    surfaceVariant = SurfaceContainerHighest,
    onSurfaceVariant = LabelSlate,
    outline = OutlineColor,
    outlineVariant = OutlineVariantColor,
    error = ErrorRed,
    errorContainer = ErrorContainerColor,
)

// DESIGN.md radius scale: 4 / 8 / 16 / 22 / 32 pill.
val FanLensShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Pill CTA (32px radius). */
val PillShape = RoundedCornerShape(32.dp)

/** Tight display type; size and spacing carry hierarchy, not weight. */
val DisplayStyle = TextStyle(
    fontSize = 24.sp,
    lineHeight = 28.sp,
    fontWeight = FontWeight.ExtraBold,
    letterSpacing = (-0.5).sp,
)

/** Uppercase technical marker. */
val LabelStyle = TextStyle(
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.3.sp,
)

/** De-emphasized metadata. */
val MetaStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
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