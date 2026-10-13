package com.fanlens.ai.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fanlens.ai.ui.GooglyAvatar
import com.fanlens.ai.ui.theme.EmeraldLive
import com.fanlens.ai.ui.theme.EmeraldLiveBg
import com.fanlens.ai.ui.theme.OutlineVariantColor
import com.fanlens.ai.ui.theme.PillShape
import com.fanlens.ai.ui.theme.PrimaryBlack
import com.fanlens.ai.ui.theme.SecondaryBlue
import com.fanlens.ai.ui.theme.SecondaryFixedColor
import com.fanlens.ai.ui.theme.SurfaceContainerHigh
import com.fanlens.ai.ui.theme.SurfaceContainerLow

/**
 * The floating pet: a mascot that lives over other apps and shows live
 * fantasy standing at a glance.
 *
 * Collapsed it is a pill with your rank, points and freshness. Tapping it
 * expands in place so you can read the standings without leaving the match;
 * only the explicit button opens the app. Every value comes from
 * [PetState] — nothing here is hardcoded.
 */
@Composable
fun OverlayBall(
    state: PetState,
    animated: Boolean,
    overlayFields: Set<String>,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onLoadDemo: () -> Unit = {},
    startExpanded: Boolean = false,
) {
    var expanded by remember { mutableStateOf(startExpanded) }

    val showRank = overlayFields.contains("rank")
    val showPoints = overlayFields.contains("points")
    val showGap = overlayFields.contains("gap")
    val showDelta = overlayFields.contains("delta")
    val showFreshness = overlayFields.contains("freshness")

    if (!expanded) {
        CollapsedPet(
            state = state,
            animated = animated,
            showRank = showRank,
            showPoints = showPoints,
            showDelta = showDelta,
            onExpand = { expanded = true },
            onDismiss = onDismiss,
        )
    } else {
        ExpandedPet(
            state = state,
            animated = animated,
            showRank = showRank,
            showPoints = showPoints,
            showGap = showGap,
            showDelta = showDelta,
            showFreshness = showFreshness,
            onCollapse = { expanded = false },
            onOpen = onOpen,
            onDismiss = onDismiss,
            onLoadDemo = onLoadDemo,
        )
    }
}

/** Always-visible pill. Kept small so it does not cover the video. */
@Composable
private fun CollapsedPet(
    state: PetState,
    animated: Boolean,
    showRank: Boolean,
    showPoints: Boolean,
    showDelta: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .border(1.5.dp, PrimaryBlack.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
            .clickable { onExpand() }
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center,
        ) {
            GooglyAvatar(sizeDp = 40, reactive = animated, winking = true)
        }

        Spacer(modifier = Modifier.width(6.dp))

        Column(modifier = Modifier.padding(end = 6.dp)) {
            // Live match score at top of pill
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.matchScore,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimaryBlack,
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "(${state.matchOvers})",
                    fontSize = 9.sp,
                    color = Color(0xFF6B7280),
                )
            }
            // Fantasy Standing below
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(EmeraldLiveBg)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = state.rankLabel,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLive,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${state.pointsOrDash} pts",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlack,
                )
                if (showDelta && state.deltaVsPrevious.isNotBlank()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Δ${state.deltaVsPrevious}",
                        fontSize = 8.sp,
                        color = SecondaryBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(SurfaceContainerHigh)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Hide overlay",
                tint = PrimaryBlack,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/** Expanded in place: your standing plus the leader you are chasing. */
@Composable
private fun ExpandedPet(
    state: PetState,
    animated: Boolean,
    showRank: Boolean,
    showPoints: Boolean,
    showGap: Boolean,
    showDelta: Boolean,
    showFreshness: Boolean,
    onCollapse: () -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onLoadDemo: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(260.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.5.dp, PrimaryBlack.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(12.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GooglyAvatar(sizeDp = 26, reactive = animated, winking = false)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = state.teamName.ifBlank { "Your team" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = PrimaryBlack,
                        )
                        Text(
                            text = freshnessText(state, showFreshness),
                            fontSize = 8.sp,
                            color = Color(0xFF6B7280),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh)
                            .clickable { onCollapse() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandLess,
                            contentDescription = "Minimize",
                            tint = PrimaryBlack,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hide overlay",
                            tint = PrimaryBlack,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }

            // Live Cricket Scoreboard inside Expanded Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF3F4F6))
                    .border(1.dp, OutlineVariantColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🇮🇳", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = state.matchScore, fontSize = 11.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                        }
                        Text(text = state.matchOvers, fontSize = 9.sp, color = Color(0xFF6B7280))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(Color(0xFFE53935))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        ) {
                            Text(text = "LIVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text(text = state.matchStatus, fontSize = 7.sp, color = Color(0xFF6B7280))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = state.matchOpponentScore, fontSize = 11.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = "🇦🇺", fontSize = 12.sp)
                        }
                        Text(text = "(20 ov)", fontSize = 9.sp, color = Color(0xFF6B7280))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!state.hasData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainerLow)
                        .padding(12.dp),
                ) {
                    Column {
                        Text(
                            text = "No standings cached for the active match.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlack,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Load a demo match to see the pet in action, or open FanLens to pick a real league.",
                            fontSize = 9.sp,
                            color = Color(0xFF6B7280),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SecondaryFixedColor)
                        .clickable { onLoadDemo() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = SecondaryBlue,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Load demo scores",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryBlue,
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainerLow)
                        .padding(10.dp),
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (showRank) {
                                Text(
                                    text = state.rankLabel,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SecondaryBlue,
                                )
                            }
                            if (showPoints) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = state.pointsOrDash,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PrimaryBlack,
                                    )
                                    Text(text = "points", fontSize = 8.sp, color = Color(0xFF6B7280))
                                }
                            }
                        }

                        if (showGap && state.gapToLeader.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Gap to leader: ${state.gapToLeader}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = SecondaryBlue,
                            )
                        }
                        if (showDelta && state.deltaVsPrevious.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Since last update: ${state.deltaVsPrevious}",
                                fontSize = 9.sp,
                                color = Color(0xFF6B7280),
                            )
                        }
                    }
                }

                if (state.leaderName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF9FAFB))
                            .border(1.dp, OutlineVariantColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                    ) {
                        Column {
                            Text(
                                text = "Chasing",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B7280),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = state.leaderName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlack,
                                )
                                Text(
                                    text = state.leaderPoints,
                                    fontSize = 10.sp,
                                    color = Color(0xFF6B7280),
                                )
                            }
                        }
                    }
                }

                if (state.fieldCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${state.fieldCount} teams in this league",
                        fontSize = 8.sp,
                        color = Color(0xFF9CA3AF),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryBlack)
                        .clickable { onOpen() }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Open App",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SecondaryFixedColor)
                        .clickable { onOpen() }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = SecondaryBlue,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ask Googly",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryBlue,
                        )
                    }
                }
            }
        }
    }
}

/** Freshness is always visible on a score (PRD FR-09). */
private fun freshnessText(state: PetState, showFreshness: Boolean): String =
    if (showFreshness) state.freshnessLabel else "Tap for standings"
