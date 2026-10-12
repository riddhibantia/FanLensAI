package com.fanlens.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fanlens.ai.data.prefs.CompanionMode
import com.fanlens.ai.data.prefs.MotionLevel
import com.fanlens.ai.data.prefs.RefreshPolicy
import com.fanlens.ai.data.prefs.TriggerCategory
import com.fanlens.ai.ui.LeagueViewModel

@Composable
fun SettingsScreen(viewModel: LeagueViewModel) {
    val settings by viewModel.settings.collectAsState()
    val scroll = rememberScrollState()
    var quietStartInput by remember { mutableStateOf((settings.quietHours.startMinutes / 60).toString()) }
    var quietEndInput by remember { mutableStateOf((settings.quietHours.endMinutes / 60).toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
    ) {
        Text("Companion mode", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Row(modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(
                selected = settings.companionMode == CompanionMode.ALWAYS_REACTIVE,
                onClick = { viewModel.setCompanionMode(CompanionMode.ALWAYS_REACTIVE) },
                label = { Text("Always Reactive") },
            )
            FilterChip(
                selected = settings.companionMode == CompanionMode.TAP_TO_ACTIVATE,
                onClick = { viewModel.setCompanionMode(CompanionMode.TAP_TO_ACTIVATE) },
                label = { Text("Tap to Activate") },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            text = when (settings.companionMode) {
                CompanionMode.ALWAYS_REACTIVE -> "Mascot may react to configured league events, within quiet hours and OS limits."
                CompanionMode.TAP_TO_ACTIVATE -> "No unsolicited animations or AI insights. Tap the ball to refresh data or ask for an explanation."
            },
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        SettingRow(
            title = "Overlay visibility",
            subtitle = "Floating mascot over other apps (best-effort, needs system permission)",
            checked = settings.overlayEnabled,
            onCheckedChange = viewModel::setOverlayEnabled,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text("Score refresh", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Row(modifier = Modifier.padding(top = 8.dp)) {
            RefreshPolicy.entries.forEach { policy ->
                FilterChip(
                    selected = settings.refreshPolicy == policy,
                    onClick = { viewModel.setRefreshPolicy(policy) },
                    label = { Text(policy.name.lowercase().replace("_", " ")) },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
        Text(
            text = "Tap-only mode does not delete or disable score data.",
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text("Proactive triggers", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Column(modifier = Modifier.padding(top = 8.dp)) {
            TriggerCategory.entries.forEach { category ->
                val selected = category in settings.triggerCategories
                FilterChip(
                    selected = selected,
                    onClick = {
                        val next = if (selected) {
                            settings.triggerCategories - category
                        } else {
                            settings.triggerCategories + category
                        }
                        viewModel.setTriggerCategories(next)
                    },
                    label = { Text(category.name.lowercase().replace("_", " ")) },
                )
            }
        }

        Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Gap threshold", color = Color.White)
            Text(" ${settings.gapThreshold} pts", color = Color.White)
            Slider(
                value = settings.gapThreshold.toFloat(),
                onValueChange = { viewModel.setGapThreshold(it.toInt()) },
                valueRange = 0f..500f,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Quiet hours", color = Color.White)
            Switch(
                checked = settings.quietHours.enabled,
                onCheckedChange = {
                    viewModel.setQuietHours(
                        it,
                        settings.quietHours.startMinutes,
                        settings.quietHours.endMinutes,
                    )
                },
            )
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = quietStartInput,
                onValueChange = { quietStartInput = it },
                label = { Text("Start hour") },
                modifier = Modifier.fillMaxWidth(0.5f),
            )
            OutlinedTextField(
                value = quietEndInput,
                onValueChange = { quietEndInput = it },
                label = { Text("End hour") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Button(
            onClick = {
                viewModel.setQuietHours(
                    settings.quietHours.enabled,
                    (quietStartInput.toIntOrNull() ?: 22) * 60,
                    (quietEndInput.toIntOrNull() ?: 7) * 60,
                )
            },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("Save quiet hours") }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text("Motion level", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Row(modifier = Modifier.padding(top = 8.dp)) {
            MotionLevel.entries.forEach { level ->
                FilterChip(
                    selected = settings.motionLevel == level,
                    onClick = { viewModel.setMotionLevel(level) },
                    label = { Text(level.name.lowercase()) },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        SettingRow(
            title = "AI explanations",
            subtitle = "Server checks every number before showing an insight; templates cover any failure",
            checked = settings.aiEnabled,
            onCheckedChange = viewModel::setAiEnabled,
        )

        Text("Tone", color = Color.White, modifier = Modifier.padding(top = 12.dp))
        Row(modifier = Modifier.padding(top = 8.dp)) {
            listOf("NEUTRAL", "BANTER", "FORMAL").forEach { tone ->
                FilterChip(
                    selected = settings.tone == tone,
                    onClick = { viewModel.setTone(tone) },
                    label = { Text(tone.lowercase()) },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.7f)) {
            Text(title, color = Color.White)
            Text(subtitle, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
