package com.fanlens.ai.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fanlens.ai.ui.theme.AmberTrophy
import com.fanlens.ai.ui.theme.BallRed
import com.fanlens.ai.ui.theme.EmeraldLive
import com.fanlens.ai.ui.theme.ErrorContainerColor
import com.fanlens.ai.ui.theme.ErrorRed
import com.fanlens.ai.ui.theme.Hairline
import com.fanlens.ai.ui.theme.LabelSlate
import com.fanlens.ai.ui.theme.LabelStyle
import com.fanlens.ai.ui.theme.OutlineVariantColor
import com.fanlens.ai.ui.theme.PillShape
import com.fanlens.ai.ui.theme.PrimaryBlack
import com.fanlens.ai.ui.theme.PrimaryContainerColor
import com.fanlens.ai.ui.theme.SecondaryBlue
import com.fanlens.ai.ui.theme.SecondaryContainerColor
import com.fanlens.ai.ui.theme.SecondaryFixedColor
import com.fanlens.ai.ui.theme.SurfaceCard
import com.fanlens.ai.ui.theme.SurfaceContainer
import com.fanlens.ai.ui.theme.SurfaceContainerHigh
import com.fanlens.ai.ui.theme.SurfaceContainerHighest
import com.fanlens.ai.ui.theme.SurfaceContainerLow
import kotlin.math.roundToInt

/**
 * High-fidelity Googly Mascot rendered via Compose Canvas:
 * Features a red 3D-shaded cricket ball with curved seam stitching,
 * white cricket cap with visor, expressive winking/celebrating eyes,
 * warm smile, and an optional auto_awesome AI badge.
 */
@Composable
fun GooglyAvatar(
    sizeDp: Int = 40,
    reactive: Boolean = false,
    showAiBadge: Boolean = false,
    winking: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "googly-transition")
    val pulseScale by if (reactive) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse-scale",
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    // Gentle bob: mascot floats up and down when reactive
    val bobOffset by if (reactive) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (-4f),
            animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "bob-offset",
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    Box(
        modifier = modifier
            .size((sizeDp * pulseScale).dp)
            .offset(y = bobOffset.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.5f, h * 0.52f)
            val radius = w * 0.44f

            // 1. Red Cricket Ball Base with 3D Radial-like Gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF5252), Color(0xFFD32F2F), Color(0xFF8B0000)),
                    center = Offset(w * 0.42f, h * 0.45f),
                    radius = radius * 1.25f,
                ),
                radius = radius,
                center = center,
            )

            // 2. White Curved Seam Stitching Line
            val seamPath = Path().apply {
                moveTo(center.x - radius * 0.85f, center.y + radius * 0.35f)
                cubicTo(
                    center.x - radius * 0.3f, center.y + radius * 0.75f,
                    center.x + radius * 0.5f, center.y + radius * 0.55f,
                    center.x + radius * 0.82f, center.y + radius * 0.15f
                )
            }
            drawPath(
                path = seamPath,
                color = Color.White.copy(alpha = 0.85f),
                style = Stroke(width = w * 0.05f, cap = StrokeCap.Round),
            )

            // Small stitch notches
            for (i in 1..4) {
                val frac = i * 0.2f
                val nx = (center.x - radius * 0.6f) + frac * (radius * 1.2f)
                val ny = center.y + radius * 0.42f - (i % 2) * (w * 0.03f)
                drawLine(
                    color = Color.White.copy(alpha = 0.75f),
                    start = Offset(nx - w * 0.02f, ny - h * 0.03f),
                    end = Offset(nx + w * 0.02f, ny + h * 0.03f),
                    strokeWidth = w * 0.03f,
                    cap = StrokeCap.Round,
                )
            }

            // 3. Eyes: Left Eye & Right Eye
            val leftEyeCenter = Offset(w * 0.38f, h * 0.48f)
            val rightEyeCenter = Offset(w * 0.62f, h * 0.48f)
            val eyeRadius = w * 0.11f

            // Left Eye
            drawCircle(color = Color.White, radius = eyeRadius, center = leftEyeCenter)
            drawCircle(color = Color(0xFF1E293B), radius = eyeRadius * 0.6f, center = Offset(leftEyeCenter.x + w * 0.02f, leftEyeCenter.y))
            drawCircle(color = Color.White, radius = eyeRadius * 0.25f, center = Offset(leftEyeCenter.x + w * 0.04f, leftEyeCenter.y - h * 0.03f))

            // Right Eye (Open or Wink)
            if (winking) {
                val winkPath = Path().apply {
                    moveTo(rightEyeCenter.x - eyeRadius, rightEyeCenter.y)
                    quadraticTo(rightEyeCenter.x, rightEyeCenter.y + eyeRadius * 0.8f, rightEyeCenter.x + eyeRadius, rightEyeCenter.y)
                }
                drawPath(path = winkPath, color = Color(0xFF1E293B), style = Stroke(width = w * 0.04f, cap = StrokeCap.Round))
            } else {
                drawCircle(color = Color.White, radius = eyeRadius, center = rightEyeCenter)
                drawCircle(color = Color(0xFF1E293B), radius = eyeRadius * 0.6f, center = Offset(rightEyeCenter.x - w * 0.01f, rightEyeCenter.y))
                drawCircle(color = Color.White, radius = eyeRadius * 0.25f, center = Offset(rightEyeCenter.x + w * 0.01f, rightEyeCenter.y - h * 0.03f))
            }

            // 4. Cheerful Smile
            val smilePath = Path().apply {
                moveTo(w * 0.42f, h * 0.67f)
                quadraticTo(w * 0.5f, h * 0.77f, w * 0.6f, h * 0.67f)
            }
            drawPath(
                path = smilePath,
                color = Color.White,
                style = Stroke(width = w * 0.045f, cap = StrokeCap.Round),
            )

            // Rosy Cheek Blushes
            drawCircle(color = Color(0xFFFF8A80).copy(alpha = 0.6f), radius = w * 0.06f, center = Offset(w * 0.28f, h * 0.61f))
            drawCircle(color = Color(0xFFFF8A80).copy(alpha = 0.6f), radius = w * 0.06f, center = Offset(w * 0.72f, h * 0.61f))

            // 5. White Cricket Cap on Head
            val capDome = Path().apply {
                moveTo(w * 0.2f, h * 0.35f)
                cubicTo(w * 0.22f, h * 0.08f, w * 0.78f, h * 0.08f, w * 0.8f, h * 0.35f)
                close()
            }
            drawPath(path = capDome, color = Color(0xFFF8FAFC), style = Fill)
            drawPath(path = capDome, color = Color(0xFFCBD5E1), style = Stroke(width = w * 0.03f))

            // Cap Visor curved outward
            val visorPath = Path().apply {
                moveTo(w * 0.16f, h * 0.33f)
                quadraticTo(w * 0.45f, h * 0.42f, w * 0.85f, h * 0.31f)
                quadraticTo(w * 0.5f, h * 0.32f, w * 0.16f, h * 0.33f)
            }
            drawPath(path = visorPath, color = Color(0xFFE2E8F0), style = Fill)
            drawPath(path = visorPath, color = Color(0xFF94A3B8), style = Stroke(width = w * 0.025f))

            // Cap button on top
            drawCircle(color = Color(0xFFE2E8F0), radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.14f))
        }

        // Optional AI AutoAwesome badge pinned at bottom right
        if (showAiBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((sizeDp * 0.38f).dp)
                    .clip(CircleShape)
                    .background(PrimaryBlack)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI",
                    tint = Color.White,
                    modifier = Modifier.size((sizeDp * 0.22f).dp),
                )
            }
        }
    }
}

/**
 * Standard MascotBall composable wrapper preserving backward compatibility.
 */
@Composable
fun MascotBall(
    sizeDp: Int,
    reactive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        GooglyAvatar(sizeDp = sizeDp, reactive = reactive)
    }
}

/**
 * Over ball chip matching HTML spec:
 * Normal runs (1, 0), boundary (4), wicket (W in error container), maximum (6 with black pill),
 * and active pulsing current ball.
 */
@Composable
fun OverBallChip(
    value: String,
    isCurrent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ball-pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha-anim",
    )

    val (bg, textColor, border) = when (value) {
        "6" -> Triple(PrimaryContainerColor, Color.White, null)
        "W" -> Triple(ErrorContainerColor, ErrorRed, null)
        "4" -> Triple(SecondaryFixedColor, SecondaryBlue, null)
        "0" -> Triple(SurfaceContainerHigh, LabelSlate, BorderStrokeData(1.dp, OutlineVariantColor.copy(alpha = 0.4f)))
        else -> if (isCurrent) {
            Triple(SurfaceCard, SecondaryBlue, BorderStrokeData(1.5.dp, SecondaryBlue, isDashed = true))
        } else {
            Triple(SurfaceContainerHigh, PrimaryBlack, BorderStrokeData(1.dp, OutlineVariantColor.copy(alpha = 0.5f)))
        }
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isCurrent) bg.copy(alpha = alphaAnim) else bg)
            .then(
                if (border != null) {
                    Modifier.border(border.width, border.color, RoundedCornerShape(6.dp))
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

private data class BorderStrokeData(
    val width: androidx.compose.ui.unit.Dp,
    val color: Color,
    val isDashed: Boolean = false,
)

/**
 * Live status chip with pulsing dot.
 */
@Composable
fun LiveStatusBadge(
    modifier: Modifier = Modifier,
    text: String = "LIVE",
) {
    val transition = rememberInfiniteTransition(label = "live-pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse-dot",
    )

    Row(
        modifier = modifier
            .clip(PillShape)
            .background(SurfaceCard)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.6f), PillShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(ErrorRed.copy(alpha = pulseAlpha)),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryBlack,
            letterSpacing = 0.5.sp,
        )
    }
}

/**
 * Quick glance Head-to-Head fantasy lead pill bar.
 */
@Composable
fun H2HRivalryBar(
    userRank: String,
    userName: String,
    userPts: String,
    leadDelta: String,
    rivalName: String,
    rivalPts: String,
    rivalRank: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceContainer)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // User
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(SecondaryContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = userRank, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = userName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = userPts, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryBlack)
        }

        // Lead Delta Pill
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(Color(0xFFFFDAD3).copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = Color(0xFFDA5C40),
                modifier = Modifier.size(13.dp),
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(text = leadDelta, color = Color(0xFF3D0600), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        // Rival
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = rivalName, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = LabelSlate)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = rivalPts, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LabelSlate)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = rivalRank, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OutlineVariantColor)
        }
    }
}

/**
 * 4-Destination shared bottom navigation bar matching the HTML mockups.
 */
@Composable
fun FanLensBottomBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard.copy(alpha = 0.98f))
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.3f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val destinations = listOf(
            Triple(0, "Matches", Icons.Default.SportsCricket),
            Triple(1, "Leagues", Icons.Default.Leaderboard),
            Triple(2, "Googly AI", Icons.Default.SmartToy),
            Triple(3, "Feed", Icons.Default.DynamicFeed),
        )

        destinations.forEach { (index, label, icon) ->
            val isSelected = currentTab == index
            val tint = if (isSelected) PrimaryBlack else LabelSlate

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                    if (index == 2) {
                        // Small red dot for Googly AI live intelligence
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ErrorRed),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = tint,
                )
            }
        }
    }
}

/**
 * Floating persistent Googly mascot buddy widget with speech bubble pill.
 */
@Composable
fun FloatingCompanionWidget(
    onClick: () -> Unit,
    onMoved: (Float, Float) -> Unit = { _, _ -> },
    initialX: Float = Float.NaN,
    initialY: Float = Float.NaN,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var offsetX by remember { mutableFloatStateOf(initialX) }
    var offsetY by remember { mutableFloatStateOf(initialY) }
    var dragging by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxX = with(density) { (maxWidth - 72.dp).toPx().coerceAtLeast(0f) }
        val maxY = with(density) { (maxHeight - 120.dp).toPx().coerceAtLeast(0f) }

        // If not initialized, place near bottom-right
        LaunchedEffect(maxWidth, maxHeight) {
            if (offsetX.isNaN() || offsetX <= 0f) {
                offsetX = (maxX - with(density) { 16.dp.toPx() }).coerceAtLeast(0f)
            }
            if (offsetY.isNaN() || offsetY <= 0f) {
                offsetY = (maxY - with(density) { 120.dp.toPx() }).coerceAtLeast(0f)
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            onMoved(offsetX.coerceIn(0f, maxX), offsetY.coerceIn(0f, maxY))
                        },
                        onDragCancel = { dragging = false },
                    ) { change, drag ->
                        change.consume()
                        offsetX = (offsetX + drag.x).coerceIn(0f, maxX)
                        offsetY = (offsetY + drag.y).coerceIn(0f, maxY)
                    }
                },
        ) {
            if (!dragging) {
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(SurfaceContainerLow)
                        .border(1.dp, PrimaryContainerColor.copy(alpha = 0.3f), PillShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "Drag me • Tap to Chat 💬",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlack,
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .border(2.dp, PrimaryContainerColor, CircleShape)
                    .clickable(enabled = !dragging, onClick = onClick)
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                GooglyAvatar(sizeDp = 42, reactive = true, winking = true)
            }
        }
    }
}

/** Freshness status badge. */
@Composable
fun FreshnessChip(freshness: String) {
    val accent = when (freshness) {
        "LIVE" -> EmeraldLive
        "DELAYED" -> AmberTrophy
        "STALE" -> ErrorRed
        "DEMO" -> SecondaryBlue
        else -> LabelSlate
    }
    Text(
        text = freshness,
        color = accent,
        style = LabelStyle,
        modifier = Modifier
            .clip(PillShape)
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.45f), PillShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Flat surface container. */
@Composable
fun FlatSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp)),
    ) {
        content()
    }
}
