package com.fanlens.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import com.fanlens.ai.ui.LeagueViewModel
import com.fanlens.ai.ui.MascotBall
import com.fanlens.ai.ui.theme.BallRed
import com.fanlens.ai.ui.theme.FanLensTheme

@Composable
fun AuthScreen(viewModel: LeagueViewModel, onSignedIn: () -> Unit) {
    val hasToken by viewModel.hasToken.collectAsState()
    var name by remember { mutableStateOf("") }

    LaunchedEffect(hasToken) {
        if (hasToken) onSignedIn()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MascotBall(sizeDp = 96, reactive = false, onClick = {})
        Text(
            text = "FanLens AI",
            color = com.fanlens.ai.ui.theme.InkOnDark,
            style = com.fanlens.ai.ui.theme.DisplayStyle,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = "Private fantasy cricket with friends",
            color = com.fanlens.ai.ui.theme.MutedSlate,
            style = com.fanlens.ai.ui.theme.MetaStyle,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.signIn(name) },
            enabled = name.isNotBlank(),
            shape = com.fanlens.ai.ui.theme.PillShape,
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
        ) {
            Text("Continue")
        }
    }
}
