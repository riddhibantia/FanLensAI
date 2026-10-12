package com.fanlens.ai.overlay

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.fanlens.ai.data.prefs.UserSettings

/** Starts/stops the overlay based on the user's settings and the permission. */
object OverlayController {
    fun sync(context: Context, settings: UserSettings) {
        val wantsOverlay = settings.overlayEnabled
        val canDraw = Settings.canDrawOverlays(context)
        when {
            wantsOverlay && canDraw -> start(context)
            else -> stop(context)
        }
    }

    private fun start(context: Context) {
        val intent = Intent(context, CompanionOverlayService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, CompanionOverlayService::class.java))
    }
}
