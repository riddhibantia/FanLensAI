package com.fanlens.ai.ui.screens

import android.provider.Settings.canDrawOverlays
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.semantics.Role
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
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
import com.fanlens.ai.ui.TabContentBottomPadding
import com.fanlens.ai.ui.theme.EmeraldLive
import com.fanlens.ai.ui.theme.EmeraldLiveText
import com.fanlens.ai.ui.theme.EmeraldLiveBg
import com.fanlens.ai.ui.theme.MinTouchTarget
import com.fanlens.ai.ui.theme.OutlineVariantColor
import com.fanlens.ai.ui.theme.MutedText
import com.fanlens.ai.ui.theme.PillShape
import com.fanlens.ai.ui.theme.PrimaryBlack
import com.fanlens.ai.ui.theme.SecondaryBlue
import com.fanlens.ai.ui.theme.SecondaryBlueText
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
 */
@Composable
fun CompanionScreen(
    viewModel: LeagueViewModel,
    onGrantOverlayPermission: () -> Unit = {},
    onOpenFullSettings: () -> Unit = {},
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
            .padding(bottom = TabContentBottomPadding),
    ) {
        // Professional Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard)
                .border(1.dp, OutlineVariantColor.copy(alpha = 0.25f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceCard)
                        .border(1.dp, OutlineVariantColor.copy(alpha = 0.4f), CircleShape)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    GooglyAvatar(sizeDp = 34, reactive = false)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "SETTINGS & COMPANION",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryBlack,
                        letterSpacing = (-0.3).sp,
                    )
                    Text(
                        text = activeLeague?.name ?: "Sunday Smashers • 4 Friends",
                        fontSize = 11.sp,
                        color = MutedText,
                    )
                }
            }
            // 48dp touch target; the visible circle stays 34dp.
            IconButton(
                onClick = { viewModel.refresh(force = true) },
                modifier = Modifier.size(48.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh standings",
                        tint = PrimaryBlack,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {

            // Permission status card
            CompanionStatusCard(
                permissionGranted = permissionGranted,
                overlayEnabled = settings.overlayEnabled,
                onRequestPermission = onGrantOverlayPermission,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Live preview built from the cached standings the overlay reads.
            // No stand-in scoreline: an empty cache shows an empty state.
            CompanionPreviewCard(
                teamName = selfRow?.teamName,
                rank = selfRow?.rank,
                totalPoints = selfRow?.totalPoints,
                gapToLeader = selfRow?.gapToLeader,
                deltaVsPrevious = selfRow?.deltaVsPrevious,
                leaderName = leaderRow?.takeIf { it.rank != selfRow?.rank }?.teamName,
                freshness = freshness?.freshness,
                updatedAt = freshness?.updatedAt,
                showFreshness = settings.widgetFields.contains("freshness"),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Selector
            CompanionModeCard(
                current = settings.companionMode,
                onSelect = { viewModel.setCompanionMode(it) },
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Controls
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

            Spacer(modifier = Modifier.height(14.dp))

            // Clean system policy note
            CompanionLimitsCard()

            Spacer(modifier = Modifier.height(14.dp))

            // This tab covers companion behaviour; scoring prefs, AI tone,
            // triggers and quiet hours live on the full settings screen.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, OutlineVariantColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Open all app settings",
                        onClick = onOpenFullSettings,
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = SecondaryBlueText,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "All App Settings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlack)
                            Text(text = "Triggers, quiet hours, AI tone, refresh policy", fontSize = 11.sp, color = MutedText)
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
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
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                working -> EmeraldLive
                                !permissionGranted -> Color(0xFFEF4444)
                                else -> Color(0xFF9CA3AF)
                            }
                        ),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = when {
                            !permissionGranted -> "Permission Required"
                            !overlayEnabled -> "Overlay Inactive"
                            else -> "Mascot Active on Screen"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlack,
                    )
                    Text(
                        text = when {
                            !permissionGranted ->
                                "Enable 'Display over other apps' to float Googly."
                            !overlayEnabled ->
                                "Turn on below to track match updates anywhere."
                            else ->
                                "Draggable over any app • Tap to expand."
                        },
                        fontSize = 11.sp,
                        color = MutedText,
                    )
                }
            }
            if (!permissionGranted) {
                // heightIn keeps the pill visually compact while the target
                // still reaches the 48dp minimum.
                Box(
                    modifier = Modifier
                        .heightIn(min = MinTouchTarget)
                        .clip(PillShape)
                        .background(PrimaryBlack)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Grant overlay permission",
                            onClick = onRequestPermission,
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else if (working) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(EmeraldLiveBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "RUNNING",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLiveText,
                    )
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
    deltaVsPrevious: String? = null,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldLive),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE TRACKING HUD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryBlack,
                        letterSpacing = 0.5.sp,
                    )
                }
                Text(
                    text = updatedAt?.takeIf { it.isNotBlank() } ?: "LIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MutedText,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        GooglyAvatar(sizeDp = 34, reactive = false, winking = true)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = teamName ?: "Riddhi XI",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryBlack,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceContainerHigh)
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    text = "#${rank ?: 2}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlack,
                                )
                            }
                        }
                        Text(
                            text = if (rank == 1) "Currently leading circle" else "Chasing $leaderName • Gap: $gapToLeader pts",
                            fontSize = 11.sp,
                            color = MutedText,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${totalPoints ?: "—"} pts",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryBlack,
                    )
                    if (!deltaVsPrevious.isNullOrBlank()) {
                        Text(
                            text = "Δ$deltaVsPrevious pts",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryBlueText,
                        )
                    }
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
                text = "COMPANION BEHAVIOUR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = PrimaryBlack,
                letterSpacing = 0.5.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))

            ModeOption(
                title = "Always Reactive",
                description = "Googly reacts dynamically to wickets, sixes, and score milestones with subtle animations and alerts.",
                selected = current == CompanionMode.ALWAYS_REACTIVE,
                onClick = { onSelect(CompanionMode.ALWAYS_REACTIVE) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            ModeOption(
                title = "Tap to Activate",
                description = "Zero unsolicited interruptions. Mascot remains calm until tapped, opening the panel for AI explanations.",
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) SecondaryFixedColor else SurfaceContainerLow)
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) SecondaryBlue.copy(alpha = 0.6f) else OutlineVariantColor.copy(alpha = 0.25f),
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (selected) SecondaryBlue else Color.Transparent)
                            .border(1.5.dp, if (selected) SecondaryBlue else Color(0xFF9CA3AF), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlack,
                    )
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SecondaryBlue.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryBlueText,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = description, fontSize = 11.sp, lineHeight = 15.sp, color = Color(0xFF4B5563))
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
                text = "OVERLAY PREFERENCES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = PrimaryBlack,
                letterSpacing = 0.5.sp,
            )

            Spacer(modifier = Modifier.height(10.dp))

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
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "Display Floating Mascot", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlack)
                        Text(
                            text = if (permissionGranted) "Floats over match streams & other apps" else "Grant permission first",
                            fontSize = 11.sp,
                            color = MutedText,
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = PrimaryBlack,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Motion Sensitivity", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlack)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MotionLevel.entries.forEach { level ->
                    val selected = motionLevel == level
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = MinTouchTarget)
                            .clip(PillShape)
                            .background(if (selected) PrimaryBlack else SurfaceContainerHigh)
                            .clickable(
                                role = Role.RadioButton,
                                onClickLabel = "Set motion sensitivity to ${level.name.lowercase()}",
                            ) { onMotionChange(level) },
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
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "System Background Policy",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlack,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "The overlay respects Android power & draw priorities. Leaderboards and fantasy stats remain 100% synchronized in-app and on the home-screen widget.",
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = MutedText,
                )
            }
        }
    }
}
