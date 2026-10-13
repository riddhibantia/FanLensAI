package com.fanlens.ai.ui.screens

import androidx.compose.ui.semantics.Role
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fanlens.ai.ui.ChatMessage
import com.fanlens.ai.ui.GooglyAvatar
import com.fanlens.ai.ui.LiveStatusBadge
import com.fanlens.ai.ui.LeagueViewModel
import com.fanlens.ai.ui.TabContentBottomPadding
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
import com.fanlens.ai.ui.theme.SecondaryContainerColor
import com.fanlens.ai.ui.theme.SecondaryFixedColor
import com.fanlens.ai.ui.theme.SurfaceBg
import com.fanlens.ai.ui.theme.SurfaceCard
import com.fanlens.ai.ui.theme.SurfaceContainer
import com.fanlens.ai.ui.theme.SurfaceContainerHigh
import com.fanlens.ai.ui.theme.SurfaceContainerHighest
import com.fanlens.ai.ui.theme.SurfaceContainerLow

@Composable
fun GooglyAiScreen(
    viewModel: LeagueViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val insight by viewModel.insight.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var selectedScenario by remember { mutableStateOf("A") }
    var selectedRuns by remember { mutableIntStateOf(6) }
    var selectedWickets by remember { mutableIntStateOf(1) }
    var chatInput by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    val standings by remember(uiState.matchId) {
        viewModel.observeStandings(uiState.matchId)
    }.collectAsState()
    val selfRow = standings.firstOrNull { it.isSelf }

    if (showApiKeyDialog) {
        AiEngineInfoDialog(
            aiEnabled = settings.aiEnabled,
            onDismiss = { showApiKeyDialog = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = TabContentBottomPadding),
    ) {
        // Top Bar
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
                    GooglyAvatar(sizeDp = 34, reactive = false, winking = true)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "GOOGLY AI", fontSize = 16.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(PrimaryBlack)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text(text = "LAB", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Text(text = "IND vs AUS • 18th Over Live", fontSize = 11.sp, color = MutedText)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Server AI status indicator. Tapping explains where the key lives.
                // The outer box is a full-size target; the visible pill stays compact.
                Box(
                    modifier = Modifier
                        .heightIn(min = MinTouchTarget)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Explain how Googly AI is configured",
                        ) { showApiKeyDialog = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (settings.aiEnabled) EmeraldLiveBg else SecondaryFixedColor)
                            .border(
                                1.dp,
                                if (settings.aiEnabled) EmeraldLive.copy(alpha = 0.4f) else SecondaryBlue.copy(alpha = 0.4f),
                                PillShape,
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (settings.aiEnabled) Icons.Default.Check else Icons.Default.Key,
                            contentDescription = "AI engine status",
                            tint = if (settings.aiEnabled) EmeraldLiveText else SecondaryBlueText,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (settings.aiEnabled) "AI On" else "AI Off",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (settings.aiEnabled) EmeraldLiveText else SecondaryBlueText,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                LiveStatusBadge()
            }
        }

        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            // Match Status Bar Sub-header
            val matchState = remember { com.fanlens.ai.data.scoring.FantasyScoringEngine.getMatchState() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(SecondaryFixedColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "IND", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SecondaryBlueText)
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = "${matchState.runs}/${matchState.wickets}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                    Text(text = " (${matchState.oversStr})", fontSize = 11.sp, color = MutedText)

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "VS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OutlineVariantColor)
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(text = "168/7", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF47464B))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "AUS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SecondaryFixedColor.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Text(text = "⚡ Target 169 • CRR 9.38", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecondaryBlueText)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hero Googly Conversational Assistant Card
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
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(modifier = Modifier.padding(end = 10.dp)) {
                            GooglyAvatar(sizeDp = 48, reactive = false, showAiBadge = true)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "GOOGLY AI ADVISOR", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SecondaryBlue))
                                }
                                Text(text = "Grounded in server data", fontSize = 10.sp, color = MutedText)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (insight.isNotBlank()) {
                                    insight
                                } else {
                                    "Ask me about your standings. I explain the numbers the server has already computed — " +
                                        "ranks, gaps and point deltas — and I will say so when I have nothing to go on."
                                },
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = PrimaryBlack,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(OutlineVariantColor.copy(alpha = 0.25f)))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Your standing:", fontSize = 11.sp, color = MutedText)
                        Text(
                            text = if (selfRow == null) {
                                "No standings yet"
                            } else {
                                "#${selfRow.rank} • ${selfRow.totalPoints} pts • " +
                                    if (selfRow.rank == 1) "leading" else "${selfRow.gapToLeader} behind"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = PrimaryBlack,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick AI Inquiry Prompts (Pills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                AiPromptPill(icon = Icons.Default.Bolt, text = "Why did my rank change?") {
                    viewModel.ask("Why did my rank change?")
                }
                Spacer(modifier = Modifier.width(6.dp))
                AiPromptPill(icon = Icons.Default.QueryStats, text = "How big is the gap to the leader?") {
                    viewModel.ask("How big is the gap to the leader?")
                }
                Spacer(modifier = Modifier.width(6.dp))
                AiPromptPill(icon = Icons.Default.AutoAwesome, text = "Which picks scored most?") {
                    viewModel.ask("Which of my picks scored the most points?")
                }
                Spacer(modifier = Modifier.width(6.dp))
                AiPromptPill(icon = Icons.Default.Replay, text = "Summarise the last few events") {
                    viewModel.ask("Summarise the most recent events in my match.")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Chat Thread with Googly AI
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
                            Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = SecondaryBlueText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "CONVERSATION WITH GOOGLY AI", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                        }
                        if (isAiThinking) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = SecondaryBlueText)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Reasoning...", fontSize = 9.sp, color = SecondaryBlueText, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    chatMessages.takeLast(4).forEach { msg ->
                        if (msg.isAi) {
                            // AI bubble
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                GooglyAvatar(sizeDp = 24, reactive = false)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp))
                                        .background(SurfaceContainerLow)
                                        .padding(10.dp),
                                ) {
                                    Column {
                                        Text(text = "Googly", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecondaryBlueText)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = msg.text, fontSize = 11.sp, color = PrimaryBlack, lineHeight = 15.sp)
                                    }
                                }
                            }
                        } else {
                            // User bubble
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 2.dp, bottomEnd = 12.dp, bottomStart = 12.dp))
                                        .background(SecondaryBlue)
                                        .padding(10.dp),
                                ) {
                                    Text(text = msg.text, fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // What-If Over Simulator Bento Section
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
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(SecondaryBlue))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "WHAT-IF OVER SIMULATOR", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceContainerLow)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(text = "WHAT-IF", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MutedText)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "PICK A SCENARIO TO ASK ABOUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedText)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Scenario tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ScenarioTab(
                            name = "Scenario A",
                            title = "Wicket",
                            badge = "Ask AI",
                            isSelected = selectedScenario == "A",
                            onClick = { selectedScenario = "A" },
                            modifier = Modifier.weight(1f),
                        )
                        ScenarioTab(
                            name = "Scenario B",
                            title = "Dot balls",
                            badge = "Ask AI",
                            isSelected = selectedScenario == "B",
                            onClick = { selectedScenario = "B" },
                            modifier = Modifier.weight(1f),
                        )
                        ScenarioTab(
                            name = "Scenario C",
                            title = "Boundaries",
                            badge = "Ask AI",
                            isSelected = selectedScenario == "C",
                            onClick = { selectedScenario = "C" },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Outcome selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Runs conceded
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceContainerLow)
                                .padding(8.dp),
                        ) {
                            Text(text = "Runs conceded", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                listOf(6, 10, 15, 20).forEach { r ->
                                    val isSel = selectedRuns == r
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = MinTouchTarget)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) PrimaryBlack else SurfaceCard)
                                            .clickable(
                                                role = Role.RadioButton,
                                                onClickLabel = "Set runs conceded to $r",
                                            ) { selectedRuns = r },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (r == 20) "20+" else "$r",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else PrimaryBlack,
                                        )
                                    }
                                }
                            }
                        }

                        // Wickets taken
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceContainerLow)
                                .padding(8.dp),
                        ) {
                            Text(text = "Wickets taken", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                listOf(0, 1, 2).forEach { w ->
                                    val isSel = selectedWickets == w
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = MinTouchTarget)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) PrimaryBlack else SurfaceCard)
                                            .clickable(
                                                role = Role.RadioButton,
                                                onClickLabel = "Set wickets taken to $w",
                                            ) { selectedWickets = w },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "${w}W",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else PrimaryBlack,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val projection = remember(selectedScenario, selectedRuns, selectedWickets) {
                        com.fanlens.ai.data.scoring.FantasyScoringEngine.projectWhatIf(selectedScenario, selectedRuns, selectedWickets)
                    }

                    // Mathematical Projection Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "PROJECTED FANTASY OUTCOME",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlack,
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (projection.projectedRank == 1) EmeraldLiveBg else SecondaryFixedColor)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "Projected Rank #${projection.projectedRank}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (projection.projectedRank == 1) EmeraldLiveText else SecondaryBlueText,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your Projected Score: ${projection.projectedPoints} pts (Δ+${projection.pointsDelta} pts)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlack,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = projection.explanation,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
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
                        // Ask AI to verify scenario
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryBlack)
                                .clickable {
                                    viewModel.ask(
                                        "Scenario $selectedScenario: if the next over went for " +
                                            "$selectedRuns runs and $selectedWickets wickets, how would my rank change against Siddhi XI?"
                                    )
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Ask Googly AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Apply scenario to match
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SecondaryBlue)
                                .clickable {
                                    viewModel.injectDemoBalls(1)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Apply to Match", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // The server's current explanation, if it has produced one.
            if (insight.isNotBlank()) {
                Text(text = "LATEST EXPLANATION", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                Spacer(modifier = Modifier.height(6.dp))
                TacticalInsightCard(
                    title = "From the server",
                    tag = "Grounded",
                    text = insight,
                )
            } else {
                Text(text = "LATEST EXPLANATION", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Text(
                        text = "No explanation yet. Ask a question below and the server will " +
                            "either answer with validated numbers or reply with a template.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MutedText,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Input AI Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceCard)
                    .border(1.dp, OutlineVariantColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = SecondaryBlueText, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    placeholder = { Text("Ask about your standings...", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    singleLine = true,
                )
                // 48dp touch target; the visible disc stays 32dp.
                IconButton(
                    onClick = {
                        if (chatInput.isNotBlank()) {
                            viewModel.ask(chatInput)
                            chatInput = ""
                        }
                    },
                    modifier = Modifier.size(MinTouchTarget),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AiEngineInfoDialog(
    aiEnabled: Boolean,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceCard)
                .border(1.dp, OutlineVariantColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(18.dp),
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SecondaryFixedColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = SecondaryBlueText, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "How Googly AI Works", fontSize = 15.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                    }
                    // 48dp hit area, 28dp visual.
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Close",
                                onClick = onDismiss,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = PrimaryBlack,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Googly AI runs on the FanLens server, not on this phone.",
                    fontSize = 12.sp,
                    color = PrimaryBlack,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp,
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• The model key lives on the server and is never stored on the device.\n" +
                        "• The server computes every score, rank and gap. The model only explains numbers it is given.\n" +
                        "• Every number in an explanation is checked against the server's own facts. Unsupported claims are rejected.\n" +
                        "• If the model is slow, unavailable, or returns something that fails validation, a plain template answer is used instead.",
                    fontSize = 11.sp,
                    color = MutedText,
                    lineHeight = 16.sp,
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (aiEnabled) EmeraldLive else Color(0xFF9CA3AF)),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (aiEnabled) "AI explanations are enabled" else "AI explanations are turned off in Settings",
                        fontSize = 11.sp,
                        color = PrimaryBlack,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlack),
                ) {
                    Text(text = "Got it", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AiPromptPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
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
        Icon(imageVector = icon, contentDescription = null, tint = SecondaryBlueText, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlack)
    }
}

@Composable
private fun ScenarioTab(
    name: String,
    title: String,
    badge: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SecondaryFixedColor.copy(alpha = 0.35f) else SurfaceContainerLow)
            .border(
                if (isSelected) 1.5.dp else 1.dp,
                if (isSelected) SecondaryBlue else OutlineVariantColor.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp),
            )
            .clickable(
                role = Role.Tab,
                onClickLabel = "Select $name $title",
                onClick = onClick,
            )
            .padding(7.dp),
    ) {
        Column {
            Text(text = name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) SecondaryBlueText else MutedText)
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
            Text(text = badge, fontSize = 9.sp, color = if (isSelected) SecondaryBlueText else MutedText)
        }
    }
}

@Composable
private fun TacticalInsightCard(title: String, tag: String, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(SurfaceContainerHigh)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(text = tag, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF47464B))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = text, fontSize = 10.sp, color = Color(0xFF47464B), lineHeight = 14.sp)
        }
    }
}
