package com.fanlens.ai.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fanlens.ai.data.scoring.CricketPlayer
import com.fanlens.ai.data.scoring.FantasyScoringEngine
import com.fanlens.ai.data.scoring.RosterMember
import com.fanlens.ai.ui.LeagueViewModel
import com.fanlens.ai.ui.theme.EmeraldLive
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

@Composable
fun RosterScreen(
    viewModel: LeagueViewModel,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    // 4 active squad members
    val initialMembers = remember {
        val existing = viewModel.getTeamRoster("team-riddhi")
        if (existing.isNotEmpty()) existing else listOf(
            RosterMember(FantasyScoringEngine.ALL_PLAYERS[0], isCaptain = true), // Kohli
            RosterMember(FantasyScoringEngine.ALL_PLAYERS[2], isViceCaptain = true), // Pandya
            RosterMember(FantasyScoringEngine.ALL_PLAYERS[1]), // Bumrah
            RosterMember(FantasyScoringEngine.ALL_PLAYERS[5]), // Maxwell
        )
    }

    val selectedPlayerIds = remember { mutableStateListOf<String>().apply { addAll(initialMembers.map { it.player.id }) } }
    var captainId by remember { mutableStateOf(initialMembers.find { it.isCaptain }?.player?.id ?: "p-kohli") }
    var viceCaptainId by remember { mutableStateOf(initialMembers.find { it.isViceCaptain }?.player?.id ?: "p-pandya") }

    val currentTotalPoints = selectedPlayerIds.sumOf { id ->
        val player = FantasyScoringEngine.ALL_PLAYERS.find { it.id == id } ?: return@sumOf 0
        val multiplier = when (id) {
            captainId -> 2.0
            viceCaptainId -> 1.5
            else -> 1.0
        }
        (player.basePoints * multiplier).toInt()
    }

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
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 48dp touch target; the visible circle stays 32dp.
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryBlack,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${settings.displayName.ifBlank { "Riddhi" }} XI LINEUP",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryBlack,
                    )
                    Text(
                        text = "IND vs AUS • 4 Player Fantasy Squad",
                        fontSize = 11.sp,
                        color = MutedText,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SecondaryFixedColor)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "$currentTotalPoints pts",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = SecondaryBlueText,
                )
            }
        }

        Column(modifier = Modifier.padding(14.dp)) {
            // Instructions Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, OutlineVariantColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SportsCricket,
                        contentDescription = null,
                        tint = SecondaryBlueText,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pick 4 Players • Set 1 Captain & 1 Vice Captain",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlack,
                        )
                        Text(
                            text = "Captain earns 2x points, Vice-Captain earns 1.5x points.",
                            fontSize = 10.sp,
                            color = MutedText,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Player Selection List
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(FantasyScoringEngine.ALL_PLAYERS, key = { it.id }) { player ->
                    val isSelected = selectedPlayerIds.contains(player.id)
                    val isCaptain = captainId == player.id
                    val isVice = viceCaptainId == player.id

                    val multiplier = when {
                        isCaptain -> 2.0
                        isVice -> 1.5
                        else -> 1.0
                    }
                    val fantasyPts = (player.basePoints * multiplier).toInt()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) SurfaceCard else SurfaceContainerLow)
                            .border(
                                1.dp,
                                if (isSelected) SecondaryBlue.copy(alpha = 0.5f) else OutlineVariantColor.copy(alpha = 0.25f),
                                RoundedCornerShape(12.dp),
                            )
                            .clickable {
                                if (isSelected) {
                                    if (selectedPlayerIds.size > 1) {
                                        selectedPlayerIds.remove(player.id)
                                        if (isCaptain) captainId = selectedPlayerIds.firstOrNull() ?: ""
                                        if (isVice) viceCaptainId = selectedPlayerIds.firstOrNull { it != captainId } ?: ""
                                    } else {
                                        Toast.makeText(context, "Must keep at least 1 player", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    if (selectedPlayerIds.size < 4) {
                                        selectedPlayerIds.add(player.id)
                                    } else {
                                        Toast.makeText(context, "Squad limit: 4 players", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
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
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (player.team == "IND") Color(0xFF1E3A8A) else Color(0xFFEAB308)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = player.team,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = player.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlack,
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(SurfaceContainerHigh)
                                                .padding(horizontal = 4.dp, vertical = 1.dp),
                                        ) {
                                            Text(
                                                text = player.role,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MutedText,
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Base: ${player.basePoints} pts • Fantasy: $fantasyPts pts",
                                        fontSize = 10.sp,
                                        color = MutedText,
                                    )
                                }
                            }

                            // C & VC Chips (only active if player is selected in squad)
                            if (isSelected) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Captain button
                                    Box(
                                        modifier = Modifier
                                            .heightIn(min = MinTouchTarget)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isCaptain) PrimaryBlack else SurfaceContainerHigh)
                                            .clickable(
                                                role = Role.RadioButton,
                                                onClickLabel = "Make ${player.name} captain",
                                            ) {
                                                captainId = player.id
                                                if (viceCaptainId == player.id) {
                                                    viceCaptainId = selectedPlayerIds.firstOrNull { it != player.id } ?: ""
                                                }
                                            }
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "C 2x",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCaptain) Color.White else PrimaryBlack,
                                        )
                                    }

                                    // Vice Captain button
                                    Box(
                                        modifier = Modifier
                                            .heightIn(min = MinTouchTarget)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isVice) SecondaryBlue else SurfaceContainerHigh)
                                            .clickable(
                                                role = Role.RadioButton,
                                                onClickLabel = "Make ${player.name} vice-captain",
                                            ) {
                                                viceCaptainId = player.id
                                                if (captainId == player.id) {
                                                    captainId = selectedPlayerIds.firstOrNull { it != player.id } ?: ""
                                                }
                                            }
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "VC 1.5x",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isVice) Color.White else PrimaryBlack,
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SurfaceContainerHigh)
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                ) {
                                    Text(text = "+ Add", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlack)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Lineup Button
            Button(
                onClick = {
                    if (selectedPlayerIds.size != 4) {
                        Toast.makeText(context, "Please select exactly 4 players", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val updatedMembers = selectedPlayerIds.map { id ->
                        val player = FantasyScoringEngine.ALL_PLAYERS.find { it.id == id }!!
                        RosterMember(
                            player = player,
                            isCaptain = id == captainId,
                            isViceCaptain = id == viceCaptainId,
                        )
                    }
                    viewModel.updateSelfRoster(updatedMembers)
                    Toast.makeText(context, "Lineup saved! Total points: $currentTotalPoints pts", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlack),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Save Lineup (${selectedPlayerIds.size}/4 Selected)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
