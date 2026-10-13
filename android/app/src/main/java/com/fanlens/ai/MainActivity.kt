package com.fanlens.ai

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import com.fanlens.ai.ui.FanLensSplash
import androidx.compose.ui.Modifier
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fanlens.ai.di.ServiceLocator
import com.fanlens.ai.di.TokenStore
import com.fanlens.ai.ui.screens.AuthScreen
import com.fanlens.ai.ui.screens.LeagueScreen
import com.fanlens.ai.ui.screens.RosterScreen
import com.fanlens.ai.ui.screens.SettingsScreen
import com.fanlens.ai.ui.theme.FanLensTheme
import com.fanlens.ai.ui.LeagueViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val leagueViewModel: LeagueViewModel by viewModels { LeagueViewModel.Factory }

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // If the user just granted the permission, auto-enable the overlay
            if (Settings.canDrawOverlays(this)) {
                leagueViewModel.setOverlayEnabled(true)
            }
            leagueViewModel.refreshOverlayState(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS,
            )
        }

        TokenStore.token = "" // resolved from DataStore below
        CoroutineScope(Dispatchers.Main).launch {
            ServiceLocator.settingsStore.settings.collect { settings ->
                TokenStore.token = settings.token
                // Always sync: turning the overlay off must stop the service.
                com.fanlens.ai.overlay.OverlayController.sync(this@MainActivity, settings)
            }
        }

        setContent {
            FanLensTheme {
                val navController = rememberNavController()
                val hasToken by leagueViewModel.hasToken.collectAsState()

                // Brand splash on cold start, drawn above the nav host so the
                // first frame is the product mark rather than a half-built
                // screen. It gates nothing: the host composes underneath it.
                var showSplash by remember { mutableStateOf(true) }

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(navController = navController, startDestination = if (hasToken) "league" else "auth") {
                        composable("auth") {
                            AuthScreen(
                                viewModel = leagueViewModel,
                                onSignedIn = {
                                    navController.navigate("league") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable("league") {
                            LeagueScreen(
                                viewModel = leagueViewModel,
                                // Settings is a top-level tab, so route the header's
                                // gear to that tab instead of pushing a second copy
                                // of the same surface on top of the stack.
                                onOpenSettings = { navController.navigate("settings") },
                                onOpenRoster = { navController.navigate("roster") },
                                onGrantOverlayPermission = { showOverlayPermissionSettings() },
                            )
                        }
                        composable("roster") {
                            RosterScreen(
                                viewModel = leagueViewModel,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = leagueViewModel,
                                onBack = {
                                    // Returning from the standalone route lands back
                                    // on the shell rather than an empty stack.
                                    if (!navController.popBackStack()) {
                                        navController.navigate("league")
                                    }
                                },
                            )
                        }
                    }

                    if (showSplash) {
                        FanLensSplash(onFinished = { showSplash = false })
                    }
                }
            }
        }
    }

    private fun showOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        )
        overlayPermissionLauncher.launch(intent)
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 1001
    }
}
