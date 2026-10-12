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
            // Permission is tracked by settings; the overlay service starts next
            // time the user enables it.
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
                            onOpenSettings = { navController.navigate("settings") },
                            onOpenRoster = { navController.navigate("roster") },
                            onGrantOverlayPermission = { showOverlayPermissionSettings() },
                        )
                    }
                    composable("roster") {
                        RosterScreen(viewModel = leagueViewModel)
                    }
                    composable("settings") {
                        SettingsScreen(viewModel = leagueViewModel)
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
