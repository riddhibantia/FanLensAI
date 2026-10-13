package com.fanlens.ai.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.fanlens.ai.data.prefs.RefreshPolicy
import com.fanlens.ai.data.prefs.TriggerCategory
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

@Composable
fun SettingsScreen(
    viewModel: LeagueViewModel,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val scroll = rememberScrollState()

    var quietStartInput by remember(settings.quietHours) { mutableStateOf((settings.quietHours.startMinutes / 60).toString()) }
    var quietEndInput by remember(settings.quietHours) { mutableStateOf((settings.quietHours.endMinutes / 60).toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLow)
                .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .border(1.dp, OutlineVariantColor.copy(alpha = 0.4f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryBlack,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "App Settings & AI Config", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            // CARD 1: AI status. The LLM key lives on the server and is never
            // entered, stored, or shipped on the device (AGENTS.md hard rule 7).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .border(1.dp, OutlineVariantColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "EXPLANATION ENGINE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                        }
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(if (settings.aiEnabled) EmeraldLiveBg else SurfaceContainerHigh)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = if (settings.aiEnabled) "Server-side" else "Off",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (settings.aiEnabled) EmeraldLive else Color(0xFF6B7280),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Googly AI runs on the FanLens server. The server holds the model key, " +
                            "checks every number in an explanation against its own standings, and falls " +
                            "back to a plain template when the model is unavailable. No API key is stored " +
                            "on this device.",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        lineHeight = 15.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CARD 2: Companion Mode
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .border(1.dp, OutlineVariantColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Column {
                    Text(text = "COMPANION MODE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.companionMode == CompanionMode.ALWAYS_REACTIVE,
                            onClick = { viewModel.setCompanionMode(CompanionMode.ALWAYS_REACTIVE) },
                            label = { Text("Always Reactive") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlack, selectedLabelColor = Color.White),
                        )
                        FilterChip(
                            selected = settings.companionMode == CompanionMode.TAP_TO_ACTIVATE,
                            onClick = { viewModel.setCompanionMode(CompanionMode.TAP_TO_ACTIVATE) },
                            label = { Text("Tap to Activate") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlack, selectedLabelColor = Color.White),
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (settings.companionMode) {
                            CompanionMode.ALWAYS_REACTIVE -> "Mascot briefly reacts to key cricket events and displays proactive insights within quiet hours."
                            CompanionMode.TAP_TO_ACTIVATE -> "Zero unsolicited animations or AI insights. Tap the mascot anytime to refresh or ask questions."
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CARD 3: Overlay Visibility & Refresh Policy
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .border(1.dp, OutlineVariantColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Cross-App Overlay Dock", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                            Text(text = "Floating mascot widget over streaming apps", fontSize = 10.sp, color = Color(0xFF6B7280))
                        }
                        Switch(
                            checked = settings.overlayEnabled,
                            onCheckedChange = viewModel::setOverlayEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SecondaryBlue),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(thickness = 1.dp, color = OutlineVariantColor.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "SCORE REFRESH POLICY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RefreshPolicy.entries.forEach { policy ->
                            val isSel = settings.refreshPolicy == policy
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.setRefreshPolicy(policy) },
                                label = { Text(policy.name.lowercase().replace("_", " ")) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SecondaryBlue, selectedLabelColor = Color.White),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CARD 4: Triggers, Gap Threshold & Quiet Hours
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .border(1.dp, OutlineVariantColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Column {
                    Text(text = "PROACTIVE TRIGGERS & SENSITIVITY", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TriggerCategory.entries.forEach { category ->
                            val selected = category in settings.triggerCategories
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val next = if (selected) settings.triggerCategories - category else settings.triggerCategories + category
                                    viewModel.setTriggerCategories(next)
                                },
                                label = { Text(category.name.lowercase().replace("_", " ")) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlack, selectedLabelColor = Color.White),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Lead Gap Threshold", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                        Text(text = "${settings.gapThreshold} pts", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryBlue)
                    }
                    Slider(
                        value = settings.gapThreshold.toFloat(),
                        onValueChange = { viewModel.setGapThreshold(it.toInt()) },
                        valueRange = 0f..200f,
                        colors = SliderDefaults.colors(thumbColor = SecondaryBlue, activeTrackColor = SecondaryBlue),
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(text = "Quiet Hours (Night Mode)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                            Text(text = "Suppress notifications and vibrations", fontSize = 10.sp, color = Color(0xFF6B7280))
                        }
                        Switch(
                            checked = settings.quietHours.enabled,
                            onCheckedChange = {
                                viewModel.setQuietHours(it, settings.quietHours.startMinutes, settings.quietHours.endMinutes)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SecondaryBlue),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CARD 5: AI Tone & Enablement
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .border(1.dp, OutlineVariantColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Enable AI Tactical Explanations", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                            Text(text = "Deterministic validation prevents hallucinated scores", fontSize = 10.sp, color = Color(0xFF6B7280))
                        }
                        Switch(
                            checked = settings.aiEnabled,
                            onCheckedChange = viewModel::setAiEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SecondaryBlue),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "GOOGLY AI MASCOT TONE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("NEUTRAL", "BANTER", "TACTICAL").forEach { tone ->
                            val isSel = settings.tone == tone
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.setTone(tone) },
                                label = { Text(tone.lowercase().replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryBlack, selectedLabelColor = Color.White),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
