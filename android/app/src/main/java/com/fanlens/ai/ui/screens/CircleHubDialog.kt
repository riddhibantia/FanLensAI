package com.fanlens.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fanlens.ai.ui.LeagueViewModel
import com.fanlens.ai.ui.theme.OutlineVariantColor
import com.fanlens.ai.ui.theme.PillShape
import com.fanlens.ai.ui.theme.PrimaryBlack
import com.fanlens.ai.ui.theme.SecondaryBlue
import com.fanlens.ai.ui.theme.SecondaryFixedColor
import com.fanlens.ai.ui.theme.SurfaceCard
import com.fanlens.ai.ui.theme.SurfaceContainerHigh

@Composable
fun CircleHubDialog(
    viewModel: LeagueViewModel,
    onDismiss: () -> Unit,
    onOpenRoster: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var newLeagueName by remember { mutableStateOf("") }
    var joinInviteCode by remember { mutableStateOf("") }
    val myLeagues by viewModel.myLeagues.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()

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
                                .background(SurfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = PrimaryBlack, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Circle Hub Management", fontSize = 15.sp, fontWeight = FontWeight.Black, color = PrimaryBlack)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = PrimaryBlack)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Existing circles, from the server.
                if (myLeagues.isNotEmpty()) {
                    Text(text = "YOUR CIRCLES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                    Spacer(modifier = Modifier.height(4.dp))
                    myLeagues.forEach { league ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(text = league.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                                Text(
                                    text = "${league.memberCount} members • ${league.teamCount} teams",
                                    fontSize = 10.sp,
                                    color = Color(0xFF6B7280),
                                )
                            }
                            if (league.id == uiState.leagueId) {
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(SecondaryFixedColor)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(text = "Active", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SecondaryBlue)
                                }
                            } else {
                                Text(
                                    text = "Tap to switch",
                                    fontSize = 9.sp,
                                    color = SecondaryBlue,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        viewModel.selectLeague(league.id)
                                        onDismiss()
                                    },
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Create League Section
                Text(text = "CREATE NEW CIRCLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = newLeagueName,
                    onValueChange = { newLeagueName = it },
                    label = { Text("Circle Name", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        if (newLeagueName.isNotBlank()) {
                            viewModel.createLeague(newLeagueName.trim())
                            onDismiss()
                        }
                    },
                    enabled = newLeagueName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlack),
                ) {
                    Text(text = "Create Circle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Join section: a code is all a friend is given.
                Text(text = "JOIN WITH INVITE CODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = joinInviteCode,
                    onValueChange = { joinInviteCode = it },
                    label = { Text("Invite Code", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.joinByCode(joinInviteCode)
                        onDismiss()
                    },
                    enabled = joinInviteCode.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape,
                ) {
                    Text(text = "Join Circle", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Tools
                Text(text = "SHORTCUTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            viewModel.injectDemoBalls(5)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Play 5 Balls", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.refresh(force = true)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = PillShape,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = PrimaryBlack, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Refresh", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenRoster()
                        },
                        modifier = Modifier.weight(1f),
                        shape = PillShape,
                    ) {
                        Text(text = "Roster", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenSettings()
                        },
                        modifier = Modifier.weight(1f),
                        shape = PillShape,
                    ) {
                        Text(text = "Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                    }
                }
            }
        }
    }
}
