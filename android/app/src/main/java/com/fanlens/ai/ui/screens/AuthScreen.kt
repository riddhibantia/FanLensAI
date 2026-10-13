package com.fanlens.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fanlens.ai.ui.GooglyAvatar
import com.fanlens.ai.ui.LeagueViewModel
import com.fanlens.ai.ui.theme.BallRed
import com.fanlens.ai.ui.theme.OutlineVariantColor
import com.fanlens.ai.ui.theme.PillShape
import com.fanlens.ai.ui.theme.PrimaryBlack
import com.fanlens.ai.ui.theme.SecondaryBlue
import com.fanlens.ai.ui.theme.SurfaceBg

@Composable
fun AuthScreen(viewModel: LeagueViewModel, onSignedIn: () -> Unit) {
    val hasToken by viewModel.hasToken.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }

    LaunchedEffect(hasToken) {
        if (hasToken) onSignedIn()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GooglyAvatar(sizeDp = 96, reactive = true, winking = true)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "FANLENS AI",
            color = PrimaryBlack,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
        )
        Text(
            text = "Dream11 Live Fantasy Companion",
            color = Color(0xFF6B7280),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SecondaryBlue,
                unfocusedBorderColor = OutlineVariantColor,
                focusedTextColor = PrimaryBlack,
                unfocusedTextColor = PrimaryBlack,
            ),
        )
        if (uiState.error.isNotBlank()) {
            Text(
                text = uiState.error,
                color = BallRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.signIn(name) },
            enabled = name.isNotBlank() && !uiState.loading,
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlack),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(
                text = if (uiState.loading) "Connecting..." else "Continue to Live Match",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}
