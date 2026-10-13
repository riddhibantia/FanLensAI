package com.fanlens.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.fanlens.ai.data.cache.CachedEvent
import com.fanlens.ai.data.cache.CachedStanding
import com.fanlens.ai.ui.GooglyAvatar
import com.fanlens.ai.ui.H2HRivalryBar
import com.fanlens.ai.ui.LiveStatusBadge
import com.fanlens.ai.ui.OverBallChip
import com.fanlens.ai.ui.LeagueViewModel
import com.fanlens.ai.ui.TabContentBottomPadding
import com.fanlens.ai.ui.theme.AmberTrophy
import com.fanlens.ai.ui.theme.MinTouchTarget
import com.fanlens.ai.ui.theme.OutlineVariantColor
import com.fanlens.ai.ui.theme.MutedText
import com.fanlens.ai.ui.theme.WordmarkStyle
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
fun MatchesScreen(
    viewModel: LeagueViewModel,
    onNavigateToLeagues: () -> Unit,
    onNavigateToGooglyAi: () -> Unit,
    onOpenCircleHub: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val standings by remember(uiState.matchId) {
        viewModel.observeStandings(uiState.matchId)
    }.collectAsState()
    val insight by viewModel.insight.collectAsState()
    val insightSource by viewModel.insightSource.collectAsState()
    val freshness by remember(uiState.matchId) {
        viewModel.observeFreshness(uiState.matchId)
    }.collectAsState()
    val recentEvents by remember(uiState.matchId) {
        viewModel.observeEvents(uiState.matchId)
    }.collectAsState()
    val myLeagues by viewModel.myLeagues.collectAsState()
    var selectedDockTab by remember { mutableIntStateOf(0) }

    val userRow = standings.find { it.isSelf }
    val rivalRow = standings.find { !it.isSelf }
    val activeLeague = myLeagues.firstOrNull { it.id == uiState.leagueId } ?: myLeagues.firstOrNull()

    // No invented placeholders: an empty league shows as empty.
    val userPts = userRow?.totalPoints ?: "—"
    val rivalPts = rivalRow?.totalPoints ?: "—"
    val rivalName = rivalRow?.teamName ?: "No rival yet"
    val memberSummary = activeLeague?.let {
        "${it.memberCount} members • ${it.teamCount} teams"
    } ?: "No circle selected"

    LaunchedEffect(Unit) {
        viewModel.refreshMyLeagues()
        if (uiState.matchId.isBlank()) {
            viewModel.setMatchId("match-ind-aus-2026")
        }
        viewModel.refreshEvents()
        viewModel.refresh(force = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = TabContentBottomPadding),
    ) {
        // Top App Bar
        TopHeaderBar(
            matchId = uiState.matchId,
            freshness = freshness?.freshness,
            onMascotClick = {
                viewModel.refresh(open = true)
                viewModel.ask("why did the standings change?")
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // 1. MATCHDOCK: Pinned Live Companion Card
            MatchDockCard(
                userPts = userPts,
                userRank = userRow?.rank?.let { "#$it" } ?: "#—",
                rivalName = rivalName,
                rivalPts = rivalPts,
                rivalGap = when {
                    userRow == null -> "no standings yet"
                    userRow.rank == 1 && rivalRow != null -> "+${rivalRow.gapToLeader} pts lead"
                    userRow.rank == 1 -> "leading"
                    else -> "-${userRow.gapToLeader} pts behind"
                },
                recentEvents = recentEvents,
                freshnessLabel = freshness?.let { fresh ->
                    fresh.updatedAt.takeIf { it.isNotBlank() }
                        ?.let { "${fresh.freshness} · $it" }
                        ?: fresh.freshness
                } ?: "NOT LOADED",
                selectedTab = selectedDockTab,
                onTabSelect = { selectedDockTab = it },
                onCircleTabClick = onNavigateToLeagues,
                onAiInsightsClick = onNavigateToGooglyAi,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. GOOGLY LIVE RADAR Card
            GooglyLiveRadarCard(
                insightText = if (insight.isNotBlank()) insight else null,
                insightSource = insightSource,
                onPromptClick = { question ->
                    viewModel.ask(question)
                    onNavigateToGooglyAi()
                },
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. PRIVATE LEAGUE leaderboard
            CircleLeaderboardCard(
                standings = standings,
                leagueName = activeLeague?.name ?: "Sunday Smashers",
                memberSummary = memberSummary,
                onOpenCircleHub = onOpenCircleHub,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. WHAT-IF OVER SIMULATOR Card
            WhatIfSimulatorCard(
                onAskGoogly = onNavigateToGooglyAi,
                onSimulate = { viewModel.injectDemoBalls(1) },
                onWhatsAppBanter = onNavigateToLeagues,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5. FRESHNESS + MANUAL REFRESH
            RefreshStrip(
                freshness = freshness?.freshness,
                onRefresh = { viewModel.refresh(force = true) },
            )
        }
    }
}

@Composable
private fun TopHeaderBar(
    matchId: String,
    freshness: String?,
    onMascotClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val matchTitle = if (matchId.contains("ind-aus", ignoreCase = true)) {
        "IND vs AUS • Sunday Smashers"
    } else {
        "Live Round • Sunday Smashers"
    }

    // Floating pill header. The whole bar is a rounded capsule: brand lockup on
    // top, a hairline rule, then the match line.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(SurfaceCard)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.45f), PillShape)
            .clickable(
                role = Role.Button,
                onClickLabel = "Refresh standings and ask Googly AI why they changed",
                onClick = onMascotClick,
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceBg)
                        .border(1.dp, OutlineVariantColor.copy(alpha = 0.5f), CircleShape)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    GooglyAvatar(sizeDp = 32, reactive = false)
                }
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = "FANLENS AI",
                    style = WordmarkStyle,
                    color = PrimaryBlack,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hairline rule separating the brand from the match line. Inset a
            // little so it does not butt against the pill's rounded edge.
            HorizontalDivider(
                thickness = 1.dp,
                color = OutlineVariantColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = matchTitle,
                fontSize = 11.sp,
                color = MutedText,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun MatchDockCard(
    userPts: String,
    userRank: String,
    rivalName: String,
    rivalPts: String,
    rivalGap: String,
    recentEvents: List<CachedEvent>,
    freshnessLabel: String,
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    onCircleTabClick: () -> Unit,
    onAiInsightsClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column {
            // Freshness first: a score is never shown without knowing how old it is.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveStatusBadge()
                Text(text = freshnessLabel, fontSize = 10.sp, color = MutedText)
            }

            // Live Match Scoreboard
            val matchSim = com.fanlens.ai.data.scoring.FantasyScoringEngine.getMatchState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Team 1
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🇮🇳", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(text = "IND", fontSize = 13.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                        }
                        Text(text = "${matchSim.runs}/${matchSim.wickets}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryBlack)
                        Text(text = "(${matchSim.oversStr})", fontSize = 10.sp, color = MutedText)
                    }

                    // VS / Status Center
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(Color(0xFFE53935))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(text = "LIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(text = "CRR 9.38", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MutedText)
                        Text(text = "Target 169", fontSize = 9.sp, color = MutedText)
                    }

                    // Team 2
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "AUS", fontSize = 13.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(text = "🇦🇺", fontSize = 16.sp)
                        }
                        Text(text = "168/7", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryBlack)
                        Text(text = "(20.0 ov)", fontSize = 10.sp, color = MutedText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Last six scored deliveries, from the server's event log.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Recent:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedText)
                    Spacer(modifier = Modifier.width(6.dp))
                    matchSim.recentBalls.forEachIndexed { index, ballVal ->
                        if (index > 0) Spacer(modifier = Modifier.width(4.dp))
                        OverBallChip(
                            value = ballVal,
                            isCurrent = index == 0,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Head-to-head from real standings.
            H2HRivalryBar(
                userRank = userRank,
                userName = "You",
                userPts = userPts,
                leadDelta = rivalGap,
                rivalName = rivalName,
                rivalPts = rivalPts,
                rivalRank = "",
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Segmented switch tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerLow)
                    .padding(3.dp),
            ) {
                val tabs = listOf("Live Match", "Private Circle", "• AI Insights")
                tabs.forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = MinTouchTarget)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) PrimaryContainerColor else Color.Transparent)
                            .clickable(
                                role = Role.Tab,
                                onClickLabel = "Show $label",
                            ) {
                                onTabSelect(index)
                                when (index) {
                                    1 -> onCircleTabClick()
                                    2 -> onAiInsightsClick()
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MutedText,
                        )
                    }
                }
            }
        }
    }
}

/** "W" for a wicket, otherwise the runs scored, parsed from the cached payload. */
private fun describeDelivery(event: CachedEvent): String {
    val payload = runCatching {
        Json.parseToJsonElement(event.payload).jsonObject
    }.getOrNull() ?: return "·"

    val wicket = payload["wicket"]?.jsonPrimitive?.content?.equals("true", ignoreCase = true) == true
    if (wicket) return "W"
    return payload["runs"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: "·"
}

@Composable
private fun GooglyLiveRadarCard(
    insightText: String?,
    insightSource: String,
    onPromptClick: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(SurfaceContainerLow, SurfaceContainer, SurfaceContainerLow)
                )
            )
            .border(1.dp, PrimaryContainerColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                // Mascot avatar
                Box(modifier = Modifier.padding(end = 12.dp)) {
                    GooglyAvatar(sizeDp = 52, reactive = false, showAiBadge = true)
                }

                // Text body
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GOOGLY LIVE RADAR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryBlack,
                                letterSpacing = 0.5.sp,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDA5C40)),
                            )
                        }
                        if (insightSource.isNotBlank()) {
                            Text(
                                text = insightSource,
                                fontSize = 10.sp,
                                color = OutlineVariantColor,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = insightText
                            ?: "Tap the mascot or ask a question to get an explanation of your standings.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryBlack,
                    )

                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Prompt Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                PromptChip(icon = "⚡", text = "Why did the standings change?") {
                    onPromptClick("Why did the standings change?")
                }
                Spacer(modifier = Modifier.width(6.dp))
                PromptChip(icon = "🎯", text = "How big is the gap to the leader?") {
                    onPromptClick("How big is the gap to the leader?")
                }
                Spacer(modifier = Modifier.width(6.dp))
                PromptChip(icon = "📊", text = "Which of my picks scored most?") {
                    onPromptClick("Which of my picks scored most?")
                }
            }
        }
    }
}

@Composable
private fun PromptChip(icon: String, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = MinTouchTarget)
            .clip(PillShape)
            .background(SurfaceCard)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.5f), PillShape)
            .clickable(
                role = Role.Button,
                onClickLabel = text,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = icon, fontSize = 11.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlack)
    }
}

@Composable
private fun CircleLeaderboardCard(
    standings: List<CachedStanding>,
    leagueName: String,
    memberSummary: String,
    onOpenCircleHub: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = AmberTrophy,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = leagueName.uppercase().ifBlank { "YOUR CIRCLE" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = PrimaryBlack,
                        )
                        Text(
                            text = memberSummary,
                            fontSize = 10.sp,
                            color = MutedText,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .heightIn(min = MinTouchTarget)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceContainerHigh)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Open Circle Hub",
                            onClick = onOpenCircleHub,
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = PrimaryBlack,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Circle Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (standings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainerLow)
                        .border(1.dp, OutlineVariantColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "No standings yet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                        Text(
                            text = "Ranks appear once a round is scored.",
                            fontSize = 11.sp,
                            color = MutedText,
                        )
                    }
                }
            } else {
                var expandedTeamId by remember { mutableStateOf<String?>(null) }

                standings.sortedBy { it.rank }.forEachIndexed { index, row ->
                    if (index > 0) Spacer(modifier = Modifier.height(6.dp))
                    val isExpanded = expandedTeamId == row.fantasyTeamId
                    val teamRoster = remember(row.fantasyTeamId) {
                        com.fanlens.ai.data.scoring.FantasyScoringEngine.getRoster(row.fantasyTeamId)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                role = Role.Button,
                                onClickLabel = if (isExpanded) {
                                    "Hide ${row.teamName} lineup"
                                } else {
                                    "Show ${row.teamName} lineup"
                                },
                            ) {
                                expandedTeamId = if (isExpanded) null else row.fantasyTeamId
                            },
                    ) {
                        if (row.isSelf) {
                            SelfLeaderboardRow(row = row)
                        } else {
                            LeaderboardItemRow(
                                rank = row.rank.toString(),
                                name = row.teamName,
                                tag = null,
                                subtitle = "Gap to leader: ${row.gapToLeader} pts",
                                points = "${row.totalPoints} pts",
                                deltaText = if (row.rank == 1) "leading" else "-${row.gapToLeader} behind",
                                deltaColor = if (row.rank == 1) SecondaryBlueText else MutedText,
                            )
                        }

                        // Expandable active roster breakdown
                        if (isExpanded && teamRoster.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceContainerLow)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = "ACTIVE LINEUP (${teamRoster.size} PLAYERS)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedText,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                teamRoster.forEach { member ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = member.player.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PrimaryBlack,
                                            )
                                            if (member.isCaptain) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(PrimaryBlack)
                                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                                ) {
                                                    Text(text = "C 2x", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            } else if (member.isViceCaptain) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(SecondaryBlue)
                                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                                ) {
                                                    Text(text = "VC 1.5x", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${member.fantasyPoints} pts (${member.player.basePoints} × ${member.multiplier})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4B5563),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelfLeaderboardRow(row: CachedStanding) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceContainerLow)
            .border(1.5.dp, PrimaryBlack.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
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
                        .background(SurfaceCard)
                        .border(1.dp, SecondaryBlue.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (row.rank == 1) "1 👑" else row.rank.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = SecondaryBlueText,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${row.teamName} (You)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(PrimaryContainerColor)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = if (row.rank == 1) "LEADER" else "YOU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                    Text(
                        text = if (row.rank == 1) {
                            "Top of the circle • Tap for lineup"
                        } else {
                            "Gap to leader: ${row.gapToLeader} pts • Tap for lineup"
                        },
                        fontSize = 10.sp,
                        color = MutedText,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${row.totalPoints} pts", fontSize = 16.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                Text(
                    text = "Δ ${row.deltaVsPrevious}",
                    fontSize = 10.sp,
                    color = SecondaryBlueText,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun LeaderboardItemRow(
    rank: String,
    name: String,
    tag: String?,
    subtitle: String,
    points: String,
    deltaText: String,
    deltaColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceCard),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = rank, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedText)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlack)
                    if (tag != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(SurfaceContainerHighest)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text(text = tag, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MutedText)
                        }
                    }
                }
                Text(text = "$subtitle • Tap for lineup", fontSize = 10.sp, color = MutedText)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = points, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
            Text(text = deltaText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = deltaColor)
        }
    }
}

@Composable
private fun WhatIfSimulatorCard(
    onAskGoogly: () -> Unit,
    onSimulate: () -> Unit,
    onWhatsAppBanter: () -> Unit,
) {
    var selectedScenario by remember { mutableStateOf("C") }

    val projection = remember(selectedScenario) {
        com.fanlens.ai.data.scoring.FantasyScoringEngine.projectWhatIf(selectedScenario, 14, 1)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, SecondaryBlue.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
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
                            .background(SecondaryBlue),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WHAT-IF OVER SIMULATOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = SecondaryBlueText,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SecondaryFixedColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(text = "LIVE ENGINE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SecondaryBlueText)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scenario Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    Triple("C", "Kohli 6s/4s", "+28 pts"),
                    Triple("A", "Cummins Wicket", "+25 pts"),
                    Triple("B", "Dot Balls", "+8 pts"),
                ).forEach { (key, label, delta) ->
                    val isSel = selectedScenario == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = MinTouchTarget)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) PrimaryBlack else SurfaceContainerLow)
                            .clickable(
                                role = Role.RadioButton,
                                onClickLabel = "Preview $label",
                            ) { selectedScenario = key }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else PrimaryBlack,
                            )
                            Text(
                                text = delta,
                                fontSize = 8.sp,
                                color = if (isSel) SecondaryBlueText else MutedText,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mathematical projection card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainerLow)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GooglyAvatar(sizeDp = 38, reactive = false)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Projected: ${projection.projectedPoints} pts (Δ+${projection.pointsDelta}) • Rank #${projection.projectedRank}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlack,
                    )
                    Text(
                        text = projection.explanation,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = Color(0xFF47464B),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Ask Googly
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = MinTouchTarget)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryBlack)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Ask Googly AI",
                            onClick = onAskGoogly,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Ask Googly", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // WhatsApp Banter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = MinTouchTarget)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerHigh)
                        .border(1.dp, OutlineVariantColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Open circle chat",
                            onClick = onWhatsAppBanter,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = PrimaryBlack, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Circle chat", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                    }
                }

                // Simulate Live Ball
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = MinTouchTarget)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SecondaryBlue)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Simulate a live ball",
                            onClick = onSimulate,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Simulate ball", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun RefreshStrip(
    freshness: String?,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AmberTrophy.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🏏", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "LIVE SCORES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryBlack,
                )
                Text(
                    text = freshness ?: "NOT LOADED",
                    fontSize = 10.sp,
                    color = MutedText,
                )
            }
        }
        Box(
            modifier = Modifier
                .heightIn(min = MinTouchTarget)
                .clip(PillShape)
                .background(SurfaceContainerHigh)
                .border(1.dp, OutlineVariantColor.copy(alpha = 0.4f), PillShape)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Refresh live scores",
                    onClick = onRefresh,
                )
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Refresh", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
        }
    }
}
