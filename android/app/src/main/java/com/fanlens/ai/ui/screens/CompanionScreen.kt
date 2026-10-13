package com.fanlens.ai.ui.screens

import android.provider.Settings.canDrawOverlays
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fanlens.ai.data.prefs.CompanionMode
import com.fanlens.ai.data.prefs.MotionLevel
import com.fanlens.ai.overlay.OverlayController
import com.fanlens.ai.ui.GooglyAvatar
import com.fanlens.ai.ui.LeagueViewModel
import com.fanlens.ai.ui.theme.EmeraldLive
import com.fanlens.ai.ui.theme.EmeraldLiveBg
import com.fanlens.ai.ui.theme.OutlineVariantColor
import com.fanlens.ai.ui.theme.PillShape
import com.fanlens.ai.ui.theme.PrimaryBlack
import com.fanlens.ai.ui.theme.SecondaryBlue
import com.fanlens.ai.ui.theme.SecondaryFixedColor
import com.fanlens.ai.ui.theme.SurfaceBg
import com.fanlens.ai.ui.theme.SurfaceCard
import com.fanlens.ai.ui.theme.SurfaceContainerHigh
import com.fanlens.ai.ui.theme.SurfaceContainerLow

/**
 * Companion controls: what the floating mascot is allowed to do, and what it
 * would currently show.
 *
 * Everything here reads real state. The preview is the same standings the
 * overlay renders, so what the user sees here is what they get on screen.
 * The overlay is best-effort: when the permission is missing the screen says
 * so and points at the working fallbacks instead of pretending to be live.
 */
@Composable
fun CompanionScreen(
    viewModel: LeagueViewModel,
    onGrantOverlayPermission: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val myLeagues by viewModel.myLeagues.collectAsState()
    val standings by remember(uiState.matchId) {
        viewModel.observeStandings(uiState.matchId)
    }.collectAsState()
    val freshness by remember(uiState.matchId) {
        viewModel.observeFreshness(uiState.matchId)
    }.collectAsState()

    val activeLeague = myLeagues.firstOrNull { it.id == uiState.leagueId }
    val selfRow = standings.firstOrNull { it.isSelf }
    val leaderRow = standings.minByOrNull { it.rank }
    val permissionGranted = canDrawOverlays(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLow)
                .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceCard)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    GooglyAvatar(sizeDp = 32, reactive = settings.overlayEnabled)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "COMPANION",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryBlack,
                    )
                    Text(
                        text = activeLeague?.name ?: "No circle selected",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                    )
                }
            }
            IconButton(
                onClick = { viewModel.refresh(force = true) },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHigh),
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Refresh standings",
                    tint = PrimaryBlack,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {

            // Permission status. Never claim the overlay is working when it is not.
            CompanionStatusCard(
                permissionGranted = permissionGranted,
                overlayEnabled = settings.overlayEnabled,
                onRequestPermission = onGrantOverlayPermission,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Live preview built from the cached standings the overlay reads.
            CompanionPreviewCard(
                teamName = selfRow?.teamName,
                rank = selfRow?.rank,
                totalPoints = selfRow?.totalPoints,
                gapToLeader = selfRow?.gapToLeader,
                leaderName = leaderRow?.takeIf { it.rank != selfRow?.rank }?.teamName,
                freshness = freshness?.freshness,
                updatedAt = freshness?.updatedAt,
                showFreshness = settings.widgetFields.contains("freshness"),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // The two modes, exactly as the PRD defines them.
            CompanionModeCard(
                current = settings.companionMode,
                onSelect = { viewModel.setCompanionMode(it) },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Controls that actually change overlay behaviour.
            CompanionControlsCard(
                overlayEnabled = settings.overlayEnabled,
                permissionGranted = permissionGranted,
                motionLevel = settings.motionLevel,
                onToggleOverlay = { enabled ->
                    viewModel.setOverlayEnabled(enabled)
                    when {
                        enabled && !permissionGranted -> onGrantOverlayPermission()
                        enabled && permissionGranted -> OverlayController.sync(context, settings.copy(overlayEnabled = true))
                        !enabled -> OverlayController.stop(context)
                    }
                },
                onGrantPermission = onGrantOverlayPermission,
                onMotionChange = { viewModel.setMotionLevel(it) },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Honest statement of what the overlay cannot do.
            CompanionLimitsCard()
        }
    }
}

@Composable
private fun CompanionStatusCard(
    permissionGranted: Boolean,
    overlayEnabled: Boolean,
    onRequestPermission: () -> Unit,
) {
    val working = permissionGranted && overlayEnabled
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (working) EmeraldLive else Color(0xFF9CA3AF)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = when {
                            !permissionGranted -> "Overlay permission not granted"
                            !overlayEnabled -> "Overlay turned off"
                            else -> "Overlay active"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlack,
                    )
                    Text(
                        text = when {
                            !permissionGranted ->
                                "Android has not allowed this app to draw over other apps."
                            !overlayEnabled ->
                                "Turn it on below to show the floating mascot."
                            else ->
                                "The mascot shows your real standings and can be dragged."
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                    )
                }
            }
            if (!permissionGranted) {
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(PrimaryBlack)
                        .clickable(onClick = onRequestPermission)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(text = "Grant", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CompanionPreviewCard(
    teamName: String?,
    rank: Int?,
    totalPoints: String?,
    gapToLeader: String?,
    leaderName: String?,
    freshness: String?,
    updatedAt: String?,
    showFreshness: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "WHAT THE MASCOT SHOWS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimaryBlack,
                )
                if (showFreshness && freshness != null) {
                    Text(
                        text = updatedAt?.takeIf { it.isNotBlank() }
                            ?.let { "$freshness · $it" } ?: freshness,
                        fontSize = 10.sp,
                        color = Color(0xFF6B7280),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (teamName == null) {
                Text(
                    text = "No standings yet",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlack,
                )
                Text(
                    text = "The mascot shows an empty state until a round is scored.",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280),
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = rank?.toString() ?: "–",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryBlack,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = teamName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlack,
                            )
                            Text(
                                text = if (rank == 1) {
                                    "Top of the circle"
                                } else {
                                    "Gap to leader: ${gapToLeader ?: "—"} pts" +
                                        (leaderName?.let { " ($it)" } ?: "")
                                },
                                fontSize = 10.sp,
                                color = Color(0xFF6B7280),
                            )
                        }
                    }
                    Text(
                        text = "${totalPoints ?: "—"} pts",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryBlack,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompanionModeCard(
    current: CompanionMode,
    onSelect: (CompanionMode) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column {
            Text(
                text = "COMPANION MODE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = PrimaryBlack,
            )
            Spacer(modifier = Modifier.height(8.dp))

            ModeOption(
                title = "Always Reactive",
                description = "The mascot may react to events you opted into, subject to " +
                    "quiet hours and cooldowns. Android may still delay it.",
                selected = current == CompanionMode.ALWAYS_REACTIVE,
                onClick = { onSelect(CompanionMode.ALWAYS_REACTIVE) },
            )
            Spacer(modifier = Modifier.height(6.dp))
            ModeOption(
                title = "Tap to Activate",
                description = "No unsolicited animations or insights. Tap the mascot to " +
                    "open the panel, refresh, or ask for an explanation. Scoring is unaffected.",
                selected = current == CompanionMode.TAP_TO_ACTIVATE,
                onClick = { onSelect(CompanionMode.TAP_TO_ACTIVATE) },
            )
        }
    }
}

@Composable
private fun ModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SecondaryFixedColor else SurfaceContainerLow)
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) SecondaryBlue.copy(alpha = 0.5f) else OutlineVariantColor.copy(alpha = 0.25f),
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (selected) SecondaryBlue else Color.Transparent)
                        .border(1.5.dp, if (selected) SecondaryBlue else Color(0xFF9CA3AF), CircleShape),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlack,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, fontSize = 10.sp, lineHeight = 14.sp, color = Color(0xFF6B7280))
        }
    }
}

@Composable
private fun CompanionControlsCard(
    overlayEnabled: Boolean,
    permissionGranted: Boolean,
    motionLevel: MotionLevel,
    onToggleOverlay: (Boolean) -> Unit,
    onGrantPermission: () -> Unit,
    onMotionChange: (MotionLevel) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column {
            Text(
                text = "OVERLAY CONTROLS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = PrimaryBlack,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Permission banner — prominently shown when permission is missing
            if (!permissionGranted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF3E0))
                        .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚠️ Permission required",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                            )
                            Text(
                                text = "Tap Grant → find FanLens → enable \"Allow display over other apps\"",
                                fontSize = 10.sp,
                                color = Color(0xFFBF360C),
                                lineHeight = 14.sp,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE65100))
                                .clickable(onClick = onGrantPermission)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "Grant",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = PrimaryBlack,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(text = "Show floating mascot", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlack)
                        Text(
                            text = if (permissionGranted) "Drag it anywhere; it stays put" else "Grant permission first",
                            fontSize = 10.sp,
                            color = Color(0xFF6B7280),
                        )
                    }
                }
                Switch(
                    checked = overlayEnabled,
                    onCheckedChange = onToggleOverlay,
                    enabled = permissionGranted,
                    colors = SwitchDefaults.colors(checkedTrackColor = EmeraldLive),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = PrimaryBlack,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Motion", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlack)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MotionLevel.entries.forEach { level ->
                    val selected = motionLevel == level
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(PillShape)
                            .background(if (selected) PrimaryBlack else SurfaceContainerHigh)
                            .clickable { onMotionChange(level) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = level.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else PrimaryBlack,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanionLimitsCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EmeraldLiveBg)
            .border(1.dp, EmeraldLive.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column {
            Text(
                text = "WHAT THE OVERLAY CANNOT DO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = EmeraldLive,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Android controls when a background app may draw or animate. The mascot " +
                    "will not react instantly to every ball, and it stops when the system kills " +
                    "the service or the battery saver engages. Your scores are never affected — " +
                    "the home-screen widget and this screen always show the same standings.",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = PrimaryBlack,
            )
        }
    }
}
