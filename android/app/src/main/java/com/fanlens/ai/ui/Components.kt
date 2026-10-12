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

/**
 * The red cricket-ball mascot. `reactive` animates (Always Reactive); in
 * TAP_TO_ACTIVATE mode the mascot never animates on its own.
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
            .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
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

@Composable
fun FreshnessChip(freshness: String) {
    val color = when (freshness) {
        "LIVE" -> Color(0xFF2E7D32)
        "DELAYED" -> Color(0xFFF9A825)
        "STALE" -> Color(0xFFC62828)
        else -> Color(0xFF616161) // DEMO
    }
    Text(
        text = freshness,
        color = Color.White,
        modifier = Modifier
            .clip(CircleShape)
            .background(color)
            .padding(horizontal = 10.dp, vertical = 2.dp),
    )
}
