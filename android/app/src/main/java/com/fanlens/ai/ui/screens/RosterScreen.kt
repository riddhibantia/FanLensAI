package com.fanlens.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fanlens.ai.data.network.RosterPlayerRequest
import com.fanlens.ai.di.ServiceLocator
import com.fanlens.ai.ui.LeagueViewModel
import com.fanlens.ai.ui.theme.EmeraldLive
import kotlinx.coroutines.launch

/** Mutable-state holder so Compose re-renders when a role changes. */
class RosterEntry(val playerId: String) {
    var role by mutableStateOf("BAT")
    var isCaptain by mutableStateOf(false)
    var isVice by mutableStateOf(false)
}

@Composable
fun RosterScreen(
    viewModel: LeagueViewModel,
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()

    var matchId by remember { mutableStateOf(settings.activeMatchId) }
    var rulesetId by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var newPlayerId by remember { mutableStateOf("") }
    val rulesets = remember { mutableStateListOf<RulesetOption>() }
    val roster = remember { mutableStateListOf<RosterEntry>() }

    LaunchedEffect(Unit) {
        ServiceLocator.rosterRepository.rulesets()
            .onSuccess { rows ->
                rulesets.clear()
                rulesets.addAll(rows.map { RulesetOption(it.id, it.name, it.version) })
                if (rulesetId.isBlank() && rows.isNotEmpty()) {
                    rulesetId = rows.first().id
                }
            }
            .onFailure { status = "could not load rulesets: " + (it.message ?: "") }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.fanlens.ai.ui.theme.SurfaceBg)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = com.fanlens.ai.ui.theme.PrimaryBlack,
                )
            }
            Text(
                "YOUR FANTASY TEAM",
                color = com.fanlens.ai.ui.theme.PrimaryBlack,
                style = com.fanlens.ai.ui.theme.LabelStyle,
            )
        }
        OutlinedTextField(
            value = matchId,
            onValueChange = { matchId = it },
            label = { Text("Match ID") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Text(
            text = "SCORING RULESET",
            modifier = Modifier.padding(top = 10.dp),
            style = com.fanlens.ai.ui.theme.LabelStyle,
        )
        if (rulesets.isEmpty()) {
            Text(
                text = "No rulesets available. The server has none configured, so a roster " +
                    "cannot be saved yet.",
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                rulesets.forEach { option ->
                    val selected = rulesetId == option.id
                    FilterChip(
                        selected = selected,
                        onClick = { rulesetId = option.id },
                        label = { Text("${option.name} (v${option.version})") },
                        modifier = Modifier.padding(end = 6.dp, bottom = 4.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newPlayerId,
                onValueChange = { newPlayerId = it },
                label = { Text("Player ID from the match feed") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedButton(
                enabled = newPlayerId.isNotBlank() && roster.none { it.playerId == newPlayerId.trim() },
                onClick = {
                    val id = newPlayerId.trim()
                    roster.add(RosterEntry(id))
                    newPlayerId = ""
                },
                modifier = Modifier.padding(start = 8.dp),
            ) { Text("Add") }
        }

        Text(
            text = "${roster.size} of $ROSTER_SIZE picked",
            color = if (roster.size == ROSTER_SIZE) EmeraldLive else Color.Gray,
            modifier = Modifier.padding(top = 4.dp),
        )

        Row(modifier = Modifier.padding(top = 8.dp)) {
            Button(
                enabled = matchId.isNotBlank() &&
                    rulesetId.isNotBlank() &&
                    roster.size == ROSTER_SIZE &&
                    roster.count { it.isCaptain } == 1 &&
                    roster.count { it.isVice } <= 1,
                onClick = {
                    status = ""
                    scope.launch {
                        ServiceLocator.rosterRepository.saveRoster(
                            leagueId = uiState.leagueId,
                            matchId = matchId,
                            rulesetId = rulesetId,
                            teamName = "${settings.displayName} XI",
                            players = roster.map {
                                RosterPlayerRequest(it.playerId, it.role, it.isCaptain, it.isVice)
                            },
                        ).onSuccess { status = "roster saved" }
                            .onFailure { status = "save failed: " + (it.message ?: "") }
                    }
                },
            ) { Text("Save roster") }
            Text(text = status, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
            items(roster, key = { it.playerId }) { entry ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(text = entry.playerId, color = Color.White)
                        listOf("BAT", "BOWL", "WK", "AR").forEach { role ->
                            FilterChip(
                                selected = entry.role == role,
                                onClick = { entry.role = role },
                                label = { Text(role) },
                            )
                        }
                        OutlinedButton(onClick = { entry.isCaptain = !entry.isCaptain }) {
                            Text(if (entry.isCaptain) "C★" else "C")
                        }
                        OutlinedButton(onClick = { entry.isVice = !entry.isVice }) {
                            Text(if (entry.isVice) "VC★" else "VC")
                        }
                        OutlinedButton(onClick = { roster.remove(entry) }) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }
}

/** The server rejects a roster that is not exactly 11 players. */
private const val ROSTER_SIZE = 11

/** A ruleset as the server describes it. */
data class RulesetOption(val id: String, val name: String, val version: String)
