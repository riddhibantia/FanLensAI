package com.fanlens.ai.ui.screens

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.heightIn
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fanlens.ai.data.cache.CachedStanding
import com.fanlens.ai.data.network.LeagueSummaryResponse
import com.fanlens.ai.ui.ChatMessage
import com.fanlens.ai.ui.GooglyAvatar
import com.fanlens.ai.ui.LiveStatusBadge
import com.fanlens.ai.ui.LeagueViewModel
import com.fanlens.ai.ui.TabContentBottomPadding
import com.fanlens.ai.ui.theme.AmberTrophy
import com.fanlens.ai.ui.theme.AmberTrophyBg
import com.fanlens.ai.ui.theme.EmeraldLive
import com.fanlens.ai.ui.theme.EmeraldLiveText
import com.fanlens.ai.ui.theme.EmeraldLiveBg
import com.fanlens.ai.ui.theme.ErrorRed
import com.fanlens.ai.ui.theme.MinTouchTarget
import com.fanlens.ai.ui.theme.OutlineVariantColor
import com.fanlens.ai.ui.theme.MutedText
import com.fanlens.ai.ui.theme.PillShape
import com.fanlens.ai.ui.theme.PrimaryBlack
import com.fanlens.ai.ui.theme.PrimaryContainerColor
import com.fanlens.ai.ui.theme.SecondaryBlue
import com.fanlens.ai.ui.theme.SecondaryBlueText
import com.fanlens.ai.ui.theme.SecondaryFixedColor
import com.fanlens.ai.ui.theme.SurfaceBg
import com.fanlens.ai.ui.theme.SurfaceCard
import com.fanlens.ai.ui.theme.SurfaceContainer
import com.fanlens.ai.ui.theme.SurfaceContainerHigh
import com.fanlens.ai.ui.theme.SurfaceContainerHighest
import com.fanlens.ai.ui.theme.SurfaceContainerLow

@Composable
fun LeaguesScreen(
    viewModel: LeagueViewModel,
    onBackToMatches: () -> Unit,
    onOpenCircleHub: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showPostMatchWrap by remember { mutableStateOf(false) }
    var banterInput by remember { mutableStateOf("") }
    val chatMessages by viewModel.chatMessages.collectAsState()

    val myLeagues by viewModel.myLeagues.collectAsState()
    val ownedInviteCode by viewModel.ownedInviteCode.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val standings by remember(uiState.matchId) {
        viewModel.observeStandings(uiState.matchId)
    }.collectAsState()

    val activeLeague = myLeagues.firstOrNull { it.id == uiState.leagueId } ?: myLeagues.firstOrNull()

    // Load the circle list once the screen appears.
    LaunchedEffect(Unit) { viewModel.refreshMyLeagues() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = TabContentBottomPadding),
    ) {
        // Top App Bar
        LeagueHeaderBar(
            leagueName = activeLeague?.name ?: "FanLens",
            subtitle = when {
                activeLeague == null -> "No circle yet • Create or join one"
                else -> "${activeLeague!!.memberCount} members • ${activeLeague!!.teamCount} teams"
            },
            onBack = onBackToMatches,
            isWrap = showPostMatchWrap,
            onToggleWrap = { showPostMatchWrap = !showPostMatchWrap },
            onShare = {
                val shareText = ownedInviteCode.takeIf { it.isNotBlank() }?.let { code ->
                    "Join my private fantasy circle '${activeLeague?.name ?: "FanLens"}' " +
                        "on FanLens AI! Invite code: $code"
                } ?: buildStandingsShareText(
                    activeLeague?.name ?: "FanLens circle",
                    standings,
                )
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Circle"))
            },
        )

        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            // Circle Switcher Bar, driven by the circles the user actually belongs to.
            CircleSwitcherBar(
                leagues = myLeagues,
                activeLeagueId = activeLeague?.id,
                onSelectCircle = { id -> viewModel.selectLeague(id) },
                onCreateOrJoin = onOpenCircleHub,
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (showPostMatchWrap) {
                // Post-Match Wrap: built from the standings on screen.
                PostMatchWrapSection(
                    standings = standings,
                    leagueName = activeLeague?.name.orEmpty(),
                    onShare = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                buildStandingsShareText(
                                    activeLeague?.name ?: "FanLens circle",
                                    standings,
                                ),
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Standings"))
                    },
                    onStartNew = { showPostMatchWrap = false },
                )
            } else {
                // Live circle summary: real membership and roster counts.
                CircleOverviewPotBanner(
                    league = activeLeague,
                    standingsCount = standings.size,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Invite card. A code is only knowable right after the circle is
                // created, so it is shown then and otherwise explains itself.
                CircleInviteCodeCard(
                    inviteCode = ownedInviteCode,
                    canInvite = activeLeague?.isOwner == true,
                    onCopy = {
                        if (ownedInviteCode.isNotBlank()) {
                            clipboardManager.setText(AnnotatedString(ownedInviteCode))
                            Toast.makeText(context, "Invite code copied", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Invite codes are shown once, at creation",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    onShareWhatsApp = {
                        val text = if (ownedInviteCode.isNotBlank()) {
                            "Join my private fantasy circle '${activeLeague?.name ?: "FanLens"}' " +
                                "on FanLens AI! Invite code: $ownedInviteCode"
                        } else {
                            buildStandingsShareText(
                                activeLeague?.name ?: "FanLens circle",
                                standings,
                            )
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, text)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Circle"))
                    },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Real standings from the cache, with per-member rosters.
                FullStandingsSection(standings = standings, leagueId = activeLeague?.id.orEmpty())

                Spacer(modifier = Modifier.height(12.dp))

                // Circle Live Banter Chat
                CircleLiveBanterCard(
                    leagueName = activeLeague?.name.orEmpty(),
                    messages = chatMessages,
                    inputText = banterInput,
                    onInputChange = { banterInput = it },
                    onSend = {
                        if (banterInput.isNotBlank()) {
                            viewModel.sendBanter(banterInput)
                            banterInput = ""
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CircleSwitcherBar(
    leagues: List<LeagueSummaryResponse>,
    activeLeagueId: String?,
    onSelectCircle: (String) -> Unit,
    onCreateOrJoin: () -> Unit,
) {
    // Horizontal scroll, not `take(2)`: a user in more than two circles could
    // not reach the rest. Each chip keeps an intrinsic width and the row pans.
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leagues.forEach { league ->
                val isSel = league.id == activeLeagueId
                Box(
                    modifier = Modifier
                        .heightIn(min = MinTouchTarget)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) PrimaryBlack else SurfaceCard)
                        .border(
                            1.dp,
                            if (isSel) PrimaryBlack else OutlineVariantColor.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp),
                        )
                        .selectable(
                            selected = isSel,
                            role = Role.Tab,
                            onClick = { onSelectCircle(league.id) },
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${league.name} (${league.memberCount})",
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) Color.White else PrimaryBlack,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .heightIn(min = MinTouchTarget)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCard)
                .border(1.dp, OutlineVariantColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Create or join a circle",
                    onClick = onCreateOrJoin,
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (leagues.isEmpty()) "Create a circle" else "+ Create/Join",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryBlack,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CircleInviteCodeCard(
    inviteCode: String,
    canInvite: Boolean,
    onCopy: () -> Unit,
    onShareWhatsApp: () -> Unit,
) {
    val hasCode = inviteCode.isNotBlank()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SecondaryFixedColor.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = SecondaryBlueText, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "CIRCLE INVITE CODE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MutedText)
                    Text(
                        text = when {
                            hasCode -> inviteCode
                            canInvite -> "Shown once, at creation"
                            else -> "Owner only"
                        },
                        fontSize = if (hasCode) 14.sp else 12.sp,
                        fontWeight = if (hasCode) FontWeight.Black else FontWeight.Medium,
                        color = PrimaryBlack,
                        letterSpacing = if (hasCode) 1.sp else 0.sp,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // heightIn(min = 48.dp) keeps the pill's compact look while
                // giving it a full-size touch target.
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(PillShape)
                        .background(SurfaceContainerHigh)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Copy invite code",
                            onClick = onCopy,
                        )
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = PrimaryBlack, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Copy", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                    }
                }
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(PillShape)
                        .background(SecondaryBlue)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = if (hasCode) "Share invite" else "Share standings",
                            onClick = onShareWhatsApp,
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hasCode) "Invite" else "Share",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeagueHeaderBar(
    leagueName: String,
    subtitle: String,
    onBack: () -> Unit,
    isWrap: Boolean,
    onToggleWrap: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.25f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = MinTouchTarget)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Back to matches",
                    onClick = onBack,
                ),
        ) {
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
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = leagueName.ifBlank { "Sunday Smashers" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryBlack,
                        letterSpacing = (-0.3).sp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SecondaryBlue)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "CIRCLE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
                Text(
                    text = if (isWrap) "Standings summary" else subtitle,
                    fontSize = 11.sp,
                    color = MutedText,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 48dp touch target; the icon itself stays 16dp.
            IconButton(onClick = onShare, modifier = Modifier.size(48.dp)) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = PrimaryBlack, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .heightIn(min = MinTouchTarget)
                    .clip(PillShape)
                    .background(SurfaceCard)
                    .border(1.dp, OutlineVariantColor.copy(alpha = 0.4f), PillShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (isWrap) "View live standings" else "Wrap up the circle",
                        onClick = onToggleWrap,
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isWrap) "View Live" else "Wrap 🏆",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlack,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            LiveStatusBadge(text = if (isWrap) "FINAL" else "LIVE")
        }
    }
}

@Composable
private fun CircleOverviewPotBanner(
    league: LeagueSummaryResponse?,
    standingsCount: Int,
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
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(SecondaryFixedColor)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = if (league?.isOwner == true) "OWNER" else "MEMBER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryBlueText,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = league?.name ?: "No circle yet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryBlack,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerLow)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    GooglyAvatar(sizeDp = 42, reactive = false)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real counts. No prize pools: this app has no money contests.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircleStatCell(
                    modifier = Modifier.weight(1f),
                    label = "Members",
                    value = league?.memberCount?.toString() ?: "0",
                )
                CircleStatCell(
                    modifier = Modifier.weight(1f),
                    label = "Teams",
                    value = league?.teamCount?.toString() ?: "0",
                )
                CircleStatCell(
                    modifier = Modifier.weight(1f),
                    label = "Ranked",
                    value = standingsCount.toString(),
                )
            }
        }
    }
}

@Composable
private fun CircleStatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceContainerLow)
            .padding(8.dp),
    ) {
        Text(text = label, fontSize = 10.sp, color = MutedText)
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
    }
}

@Composable
private fun FullStandingsSection(
    standings: List<CachedStanding>,
    leagueId: String,
) {
    var expandedMember by remember { mutableStateOf<String?>(null) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "FULL CIRCLE STANDINGS (${standings.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = PrimaryBlack,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (standings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "No standings yet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                    Text(
                        text = if (leagueId.isBlank()) {
                            "Create or join a circle to get started."
                        } else {
                            "Ranks appear once a round is scored."
                        },
                        fontSize = 11.sp,
                        color = MutedText,
                    )
                }
            }
        } else {
            standings.sortedBy { it.rank }.forEachIndexed { index, row ->
                val key = row.teamName
                ExpandableMemberCard(
                    rank = row.rank.toString(),
                    name = if (row.isSelf) "${row.teamName} (You)" else row.teamName,
                    tag = when {
                        row.rank == 1 -> "LEADER"
                        row.isSelf -> "YOU"
                        else -> null
                    },
                    points = row.totalPoints,
                    delta = if (row.rank == 1) {
                        "leading"
                    } else {
                        "−${row.gapToLeader} behind"
                    },
                    deltaColor = if (row.isSelf) SecondaryBlueText else MutedText,
                    summary = "Gap to leader: ${row.gapToLeader} pts",
                    isExpanded = expandedMember == key,
                    onToggle = { expandedMember = if (expandedMember == key) null else key },
                    isLeader = row.rank == 1,
                )
                if (index != standings.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun ExpandableMemberCard(
    rank: String,
    name: String,
    tag: String?,
    points: String,
    delta: String,
    deltaColor: Color,
    summary: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isLeader: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(
                if (isLeader) 2.dp else 1.dp,
                if (isLeader) SecondaryBlue else OutlineVariantColor.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp),
            )
            .clickable(
                role = Role.Button,
                onClickLabel = if (isExpanded) "Hide team breakdown" else "Show team breakdown",
                onClick = onToggle,
            )
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
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isLeader) SecondaryBlue else SurfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = rank,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isLeader) Color.White else PrimaryBlack,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                            if (tag != null) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(SecondaryFixedColor)
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                ) {
                                    Text(text = tag, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SecondaryBlueText)
                                }
                            }
                        }
                        Text(text = summary, fontSize = 10.sp, color = MutedText)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "$points pts", fontSize = 14.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                        Text(text = delta, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = deltaColor)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

        }
    }
}

@Composable
private fun CircleLiveBanterCard(
    leagueName: String,
    messages: List<ChatMessage>,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
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
                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = PrimaryBlack, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "CIRCLE LIVE BANTER", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                }
                Text(text = "5 Friends Online", fontSize = 10.sp, color = EmeraldLiveText, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Message Stream
            messages.takeLast(5).forEach { msg ->
                if (msg.isAi) {
                    // AI Bot message
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, SecondaryBlue.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = SecondaryBlueText, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = msg.author, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecondaryBlueText)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = msg.text, fontSize = 11.sp, color = PrimaryBlack, lineHeight = 15.sp)
                        }
                    }
                } else if (msg.isUser) {
                    // User Message
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SecondaryFixedColor.copy(alpha = 0.5f))
                            .border(1.dp, SecondaryBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = msg.author, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecondaryBlueText)
                                Text(text = msg.timestamp, fontSize = 9.sp, color = MutedText)
                            }
                            Text(text = msg.text, fontSize = 11.sp, color = PrimaryBlack)
                        }
                    }
                } else {
                    // Rival Message
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .padding(8.dp),
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = msg.author, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                                Text(text = msg.timestamp, fontSize = 9.sp, color = MutedText)
                            }
                            Text(text = msg.text, fontSize = 11.sp, color = PrimaryBlack)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Banter input bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = { Text("Message ${leagueName.ifBlank { "your circle" }}...", fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryBlue,
                        unfocusedBorderColor = OutlineVariantColor.copy(alpha = 0.5f),
                    ),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(6.dp))
                // 48dp touch target; the visible disc stays 40dp.
                IconButton(
                    onClick = onSend,
                    modifier = Modifier.size(MinTouchTarget),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryContainerColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PostMatchWrapSection(
    standings: List<CachedStanding>,
    leagueName: String,
    onShare: () -> Unit,
    onStartNew: () -> Unit,
) {
    val podium = standings.sortedBy { it.rank }.take(3)
    val winner = podium.firstOrNull()
    val selfRow = standings.firstOrNull { it.isSelf }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(18.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(AmberTrophyBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = AmberTrophy, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = leagueName.uppercase().ifBlank { "CIRCLE" } + " STANDINGS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    GooglyAvatar(sizeDp = 64, reactive = false, winking = true)
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (winner == null) {
                    Text(
                        text = "No scores yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryBlack,
                    )
                    Text(
                        text = "Standings appear once events are scored.",
                        fontSize = 12.sp,
                        color = MutedText,
                    )
                } else {
                    Text(
                        text = "🏆 ${winner.teamName} leads",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryBlack,
                    )
                    Text(
                        text = "${winner.totalPoints} Points" +
                            (selfRow?.let { " • You are #${it.rank}" } ?: ""),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryBlueText,
                    )
                }

                if (podium.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        podium.forEachIndexed { index, row ->
                            val medal = when (index) {
                                0 -> "🥇 1ST"
                                1 -> "🥈 2ND"
                                else -> "🥉 3RD"
                            }
                            val isFirst = index == 0
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isFirst) AmberTrophyBg else SurfaceContainerLow)
                                    .then(
                                        if (isFirst) {
                                            Modifier.border(
                                                1.dp,
                                                AmberTrophy.copy(alpha = 0.4f),
                                                RoundedCornerShape(10.dp),
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = medal,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFirst) AmberTrophy else MutedText,
                                    )
                                    Text(
                                        text = row.teamName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlack,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = "${row.totalPoints} pts",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PrimaryBlack,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .clip(PillShape)
                .background(PrimaryBlack)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Share circle summary",
                    onClick = onShare,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Share Circle Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .clip(PillShape)
                .background(SurfaceContainerHigh)
                .border(1.dp, OutlineVariantColor.copy(alpha = 0.4f), PillShape)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Back to live circle",
                    onClick = onStartNew,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = PrimaryBlack, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Back to Live Circle", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
            }
        }
    }
}

/** Builds a share text from the standings that are actually on screen. */
private fun buildStandingsShareText(
    leagueName: String,
    standings: List<CachedStanding>,
): String {
    val header = "🏏 $leagueName standings on FanLens AI"
    val lines = standings.sortedBy { it.rank }.take(5).map { row ->
        "${row.rank}. ${row.teamName} — ${row.totalPoints} pts"
    }
    return if (lines.isEmpty()) header else (listOf(header) + lines).joinToString("\n")
}
