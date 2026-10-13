package com.fanlens.ai.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fanlens.ai.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Brings the pet back after a reboot.
 *
 * Only restarts when the user had the overlay switched on, and OverlayController
 * still refuses to start it without the draw-over-apps permission — so a reboot
 * cannot conjure an overlay the user did not ask for.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                ServiceLocator.init(appContext)
                val settings = ServiceLocator.settingsStore.settings.first()
                if (settings.overlayEnabled) {
                    OverlayController.sync(appContext, settings)
                }
            } catch (_: Throwable) {
                // Best-effort: a failed restart just means the pet stays hidden
                // until the user opens the app. Never crash on boot.
            } finally {
                pending.finish()
            }
        }
    }
}
