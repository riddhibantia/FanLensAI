package com.fanlens.ai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fanlens.ai.R

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

/**
 * Secondary text neutral. This was previously written inline as `Color(0xFF6B7280)`
 * in ~15 places across the screens; it lives here so the value stays consistent.
 * Passes 4.5:1 against SurfaceBg and SurfaceCard.
 */
val MutedText = Color(0xFF6B7280)
val MutedTextSubtle = Color(0xFF8A8A99)

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

/**
 * Accessible text variants of the status accents.
 *
 * `EmeraldLive` on `EmeraldLiveBg` is ~2.6:1 and `SecondaryBlue` on
 * `SecondaryFixedColor` is ~4.1:1 — both below the 4.5:1 needed for body text.
 * These darker variants are for text and icons *on* the tinted backgrounds;
 * keep the brighter originals for fills, dots and borders, where contrast
 * requirements do not apply.
 */
val EmeraldLiveText = Color(0xFF047857)
val SecondaryBlueText = Color(0xFF003E92)
val AmberTrophyText = Color(0xFF92400E)

/**
 * "Behind by N" indicators in the head-to-head rivalry bar. Warm red family,
 * deliberately distinct from `ErrorRed`, which is reserved for failures.
 */
val LeadDeltaBg = Color(0xFFFFDAD3)
val LeadDeltaIcon = Color(0xFFDA5C40)
val LeadDeltaText = Color(0xFF3D0600)

/** Small attention dot on the Googly AI tab when it is the selected tab. */
val TabAlertDotActive = Color(0xFFFF5252)

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

/**
 * Brand wordmark face.
 *
 * Montserrat is bundled in `res/font` rather than fetched at runtime, so the
 * wordmark renders identically with no network — this app ships as a single
 * offline APK. Licensed under the SIL Open Font License 1.1; the licence text
 * is bundled alongside the font files.
 *
 * Used only for the "FANLENS AI" wordmark. Everything else stays on the system
 * face so body copy keeps native rendering and the smallest APK footprint.
 */
val WordmarkFont = FontFamily(
    Font(R.font.montserrat_extrabold, FontWeight.ExtraBold),
    Font(R.font.montserrat_black, FontWeight.Black),
)

/** The "FANLENS AI" wordmark: bundled face, tight tracking, heavy weight. */
val WordmarkStyle = TextStyle(
    fontFamily = WordmarkFont,
    fontSize = 19.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.Black,
    letterSpacing = 0.2.sp,
)

/** Pill CTA (32px radius). */
val PillShape = RoundedCornerShape(32.dp)

/**
 * Type scale.
 *
 * The screens previously declared `fontSize` and `fontWeight` inline at every
 * call site, which drifted into 14 distinct sizes and 64 uses of
 * Black/ExtraBold — the smallest text was the heaviest, so nothing read as
 * more important than anything else.
 *
 * Five steps, weight capped at SemiBold below 12sp. Hierarchy comes from size
 * and spacing. Use these instead of new inline values.
 */
object FanLensType {
    /** Hero numbers and screen-defining figures. */
    val display = TextStyle(
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp,
    )

    /** Screen titles and card headings. */
    val title = TextStyle(
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
    )

    /** Section headings inside cards. */
    val section = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    )

    /** Default reading size for body copy. */
    val body = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    )

    /** Supporting copy and captions. */
    val meta = TextStyle(
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    )

    /**
     * Smallest step. Uppercase technical markers only — never for running copy
     * or values the user has to compare. SemiBold, not Black: at this size
     * heavy weights close up the counters and hurt legibility.
     */
    val label = TextStyle(
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
    )
}

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

/**
 * Minimum touch target, per the Android accessibility guidance. Interactive
 * elements may draw smaller than this, but the tappable area must not be.
 */
val MinTouchTarget = 48.dp

@Composable
fun FanLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FanLensColorScheme,
        shapes = FanLensShapes,
        content = content,
    )
}