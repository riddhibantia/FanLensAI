package com.fanlens.ai.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fanlens.ai.ui.theme.BallRed
import com.fanlens.ai.ui.theme.Hairline
import com.fanlens.ai.ui.theme.LabelStyle
import com.fanlens.ai.ui.theme.PillShape
import com.fanlens.ai.ui.theme.RuleStrong

/**
 * The red cricket-ball mascot — the product's identity, kept as-is.
 * `reactive` animates (Always Reactive); in TAP_TO_ACTIVATE mode the mascot
 * never animates on its own.
 */
@Composable
fun MascotBall(
    sizeDp: Int,
    reactive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = if (reactive) {
        val transition = rememberInfiniteTransition(label = "ball-pulse")
        val animated by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "ball-scale",
        )
        animated
    } else {
        1f
    }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size((sizeDp * scale).dp)
            .clip(CircleShape)
            .background(BallRed)
            .border(1.dp, RuleStrong, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "🏏", fontSize = (sizeDp / 3).sp)
    }
}

/**
 * Freshness status. DESIGN.md restraint: hairline pill, flat fill, uppercase
 * label — no heavy chips or saturated backgrounds.
 */
@Composable
fun FreshnessChip(freshness: String) {
    val accent = when (freshness) {
        "LIVE" -> Color(0xFF69F0AE)
        "DELAYED" -> SelfGoldTone
        "STALE" -> Color(0xFFFF8A80)
        "DEMO" -> Color(0xFF8AB4F8)
        else -> MutedTone
    }
    Text(
        text = freshness,
        color = accent,
        style = LabelStyle,
        modifier = Modifier
            .clip(PillShape)
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.45f), PillShape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

private val SelfGoldTone = Color(0xFFFFC24B)
private val MutedTone = Color(0xFF93939F)

/** Flat, hairline-bordered surface: DESIGN.md's containment default (no shadow). */
@Composable
fun FlatSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(com.fanlens.ai.ui.theme.FanLensShapes.medium)
            .background(com.fanlens.ai.ui.theme.PitchMid.copy(alpha = 0.5f))
            .border(1.dp, Hairline, com.fanlens.ai.ui.theme.FanLensShapes.medium),
    ) {
        content()
    }
}