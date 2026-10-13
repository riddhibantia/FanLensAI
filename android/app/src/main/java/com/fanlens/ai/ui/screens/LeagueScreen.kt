package com.fanlens.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fanlens.ai.ui.FanLensBottomBar
import com.fanlens.ai.ui.FloatingCompanionWidget
import com.fanlens.ai.ui.LeagueViewModel
import com.fanlens.ai.ui.theme.SurfaceBg

@Composable
fun LeagueScreen(
    viewModel: LeagueViewModel,
    onOpenSettings: () -> Unit,
    onOpenRoster: () -> Unit,
    onGrantOverlayPermission: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by remember { mutableIntStateOf(0) }
    var showCircleHub by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        if (uiState.error.isNotBlank()) {
            snackbarHostState.showSnackbar(uiState.error)
        }
    }

    LaunchedEffect(uiState.message) {
        if (uiState.message.isNotBlank()) {
            snackbarHostState.showSnackbar(uiState.message)
        }
    }

    Scaffold(
        bottomBar = {
            FanLensBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SurfaceBg,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Tab content
            when (currentTab) {
                0 -> MatchesScreen(
                    viewModel = viewModel,
                    onNavigateToLeagues = { currentTab = 1 },
                    onNavigateToGooglyAi = { currentTab = 2 },
                    onOpenCircleHub = { showCircleHub = true },
                    onOpenRoster = onOpenRoster,
                    onOpenSettings = onOpenSettings,
                )
                1 -> LeaguesScreen(
                    viewModel = viewModel,
                    onBackToMatches = { currentTab = 0 },
                    onOpenCircleHub = { showCircleHub = true },
                )
                2 -> GooglyAiScreen(
                    viewModel = viewModel,
                )
                3 -> CompanionScreen(
                    viewModel = viewModel,
                    onGrantOverlayPermission = onGrantOverlayPermission,
                )
            }

            // Floating mascot: drag it anywhere, tap it to open Googly AI.
            val settings by viewModel.settings.collectAsState()
            FloatingCompanionWidget(
                onClick = {
                    currentTab = 2 // Opens Googly AI Lab
                },
                initialX = settings.overlayX.toFloat(),
                initialY = settings.overlayY.toFloat(),
                onMoved = { x, y -> viewModel.setPetPosition(x, y) },
                modifier = Modifier.fillMaxSize(),
            )

            // Loading indicator
            if (uiState.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            // Circle Hub dialog
            if (showCircleHub) {
                CircleHubDialog(
                    viewModel = viewModel,
                    onDismiss = { showCircleHub = false },
                    onOpenRoster = onOpenRoster,
                    onOpenSettings = onOpenSettings,
                )
            }
        }
    }
}
