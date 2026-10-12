package com.fanlens.ai.ui.screens

import android.content.Context
import android.os.Build
import android.provider.Settings.canDrawOverlays
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fanlens.ai.data.cache.CachedStanding
import com.fanlens.ai.data.prefs.CompanionMode
import com.fanlens.ai.overlay.OverlayController
import com.fanlens.ai.ui.FreshnessChip
import com.fanlens.ai.ui.LeagueViewModel
import com.fanlens.ai.ui.MascotBall

@Composable
fun LeagueScreen(
    viewModel: LeagueViewModel,
    onOpenSettings: () -> Unit,
    onOpenRoster: () -> Unit,
    onGrantOverlayPermission: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val standings by remember(uiState.matchId) {
        viewModel.observeStandings(uiState.matchId)
    }.collectAsState()
    val freshness by remember(uiState.matchId) {
        viewModel.observeFreshness(uiState.matchId)
    }.collectAsState()
    val insight by viewModel.insight.collectAsState()
    val events by remember(uiState.matchId) {
        viewModel.observeEvents(uiState.matchId)
    }.collectAsState()

    var leagueInput by remember { mutableStateOf("") }
    var inviteInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MascotBall(
                sizeDp = 56,
                reactive = settings.proactiveAllowed(),
                onClick = {
                    if (settings.companionMode == CompanionMode.TAP_TO_ACTIVATE) {
                        viewModel.refresh(open = true)
                    }
                    viewModel.ask("why did the standings change?")
                },
            )
            Text(
                text = settings.displayName.ifBlank { "FanLens" },
                color = Color.White,
                modifier = Modifier.padding(start = 12.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = if (settings.companionMode == CompanionMode.ALWAYS_REACTIVE) "Always Reactive" else "Tap to Activate",
                    color = Color.Gray,
                )
            }
        }

        // Freshness is always visible next to any scores.
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FreshnessChip(com.fanlens.ai.data.prefs.Freshness.normalize(freshness?.freshness))
            Text(
                text = com.fanlens.ai.data.prefs.Freshness.label(
                    freshness?.freshness ?: "", freshness?.updatedAt,
                ),
                color = Color.Gray,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (!uiState.loading && standings.isEmpty()) {
            Text(
                text = "No cached scores yet — play the demo or refresh. The widget shows the same cached data.",
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // League setup
        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("League", color = Color.White)
                OutlinedTextField(
                    value = leagueInput,
                    onValueChange = { leagueInput = it },
                    label = { Text("League ID (join)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
                OutlinedTextField(
                    value = inviteInput,
                    onValueChange = { inviteInput = it },
                    label = { Text("Invite code") },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    Button(
                        onClick = {
                            viewModel.createLeague("Sunday Smashers")
                        },
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text("Create") }
                    Button(
                        onClick = {
                            viewModel.joinLeague(leagueInput, inviteInput)
                        },
                    ) { Text("Join") }
                }
            }
        }

        // Demo controls + refresh policy (scrolls horizontally on narrow screens).
        Row(
            modifier = Modifier
                .padding(top = 12.dp)
                .horizontalScroll(rememberScrollState()),
        ) {
            Button(
                onClick = { viewModel.injectDemoBalls(5) },
                modifier = Modifier.padding(end = 8.dp),
            ) { Text("Play demo") }
            OutlinedButton(onClick = { viewModel.refresh(force = true) }) { Text("Refresh") }
            OutlinedButton(onClick = onOpenRoster, modifier = Modifier.padding(start = 8.dp)) {
                Text("Roster")
            }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.padding(start = 8.dp)) {
                Text("Settings")
            }
        }

        // Overlay toggle with permission gate
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Overlay", color = Color.White)
            androidx.compose.material3.Switch(
                checked = settings.overlayEnabled,
                onCheckedChange = { enabled ->
                    viewModel.setOverlayEnabled(enabled)
                    if (enabled && !canDrawOverlays(context)) onGrantOverlayPermission()
                    OverlayController.sync(context, settings.copy(overlayEnabled = enabled))
                },
            )
            Text(
                text = when {
                    !canDrawOverlays(context) -> "permission needed"
                    settings.overlayEnabled -> "on"
                    else -> "off"
                },
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        if (insight.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(
                    text = insight,
                    color = Color.White,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        if (uiState.loading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }
        if (uiState.message.isNotBlank()) {
            Text(text = uiState.message, color = Color(0xFF69F0AE), modifier = Modifier.padding(top = 8.dp))
        }
        if (uiState.error.isNotBlank()) {
            Text(text = uiState.error, color = Color(0xFFFF8A80), modifier = Modifier.padding(top = 8.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
            items(standings) { row -> StandingRowItem(row) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Recent events", color = Color.White, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { viewModel.refreshEvents() }) { Text("Load") }
                }
            }
            items(events.takeLast(10).reversed()) { event ->
                Text(
                    text = "#${event.sequence} ${event.eventType} · ${event.status}",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun StandingRowItem(row: CachedStanding) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#${row.rank}",
                color = if (row.isSelf) com.fanlens.ai.ui.theme.SelfGold else Color.Gray,
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = row.teamName + if (row.isSelf) " (you)" else "",
                    color = Color.White,
                )
                Text(text = "${row.totalPoints} pts · gap ${row.gapToLeader}", color = Color.Gray)
            }
            Text(
                text = if (row.deltaVsPrevious.toDoubleOrNull() ?: 0.0 >= 0) {
                    "+${row.deltaVsPrevious}"
                } else {
                    row.deltaVsPrevious
                },
                color = if ((row.deltaVsPrevious.toDoubleOrNull() ?: 0.0) >= 0) Color(0xFF69F0AE) else Color(0xFFFF8A80),
            )
        }
    }
}
