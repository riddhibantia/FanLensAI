package com.fanlens.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.fanlens.ai.data.network.FanLensApi
import com.fanlens.ai.data.network.RosterPlayerRequest
import com.fanlens.ai.di.ServiceLocator
import com.fanlens.ai.ui.LeagueViewModel
import kotlinx.coroutines.launch

/** Mutable-state holder so Compose re-renders when a role changes. */
class RosterEntry(val playerId: String) {
    var role by mutableStateOf("BAT")
    var isCaptain by mutableStateOf(false)
    var isVice by mutableStateOf(false)
}

@Composable
fun RosterScreen(viewModel: LeagueViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()

    var matchId by remember { mutableStateOf(settings.activeMatchId) }
    var rulesetId by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val roster = remember { mutableStateListOf< RosterEntry>().apply { addAll(playerList()) } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your fantasy team", color = Color.White)
        OutlinedTextField(
            value = matchId,
            onValueChange = { matchId = it },
            label = { Text("Match ID") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = rulesetId,
            onValueChange = { rulesetId = it },
            label = { Text("Ruleset ID") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Row(modifier = Modifier.padding(top = 8.dp)) {
            Button(
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
                    }
                }
            }
        }
    }
}

private fun playerList(): List<RosterEntry> = buildList {
    addAll((1..5).map { RosterEntry("p$it").apply { role = "BAT" } })
    addAll((6..9).map { RosterEntry("p$it").apply { role = "BOWL" } })
    add(RosterEntry("p10").apply { role = "WK" })
    add(RosterEntry("p11").apply { role = "AR" })
    first().isCaptain = true
}
