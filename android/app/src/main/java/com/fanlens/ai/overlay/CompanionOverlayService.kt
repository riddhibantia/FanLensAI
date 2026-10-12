package com.fanlens.ai.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.fanlens.ai.MainActivity
import com.fanlens.ai.R
import com.fanlens.ai.data.prefs.CompanionMode
import com.fanlens.ai.data.prefs.MotionLevel
import com.fanlens.ai.data.prefs.UserSettings
import com.fanlens.ai.di.ServiceLocator
import com.fanlens.ai.ui.MascotBall
import com.fanlens.ai.ui.theme.FanLensTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The cross-app mascot. Best-effort by design: the service stops when the user
 * turns the overlay off, dismisses the ball, or revokes the permission.
 */
class CompanionOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    /** Whether the mascot may animate right now: reactive mode, gate open, motion on. */
    private var allowAnimation = mutableStateOf(false)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, buildNotification())
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        observeSettings()
        addOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun observeSettings() {
        scope.launch {
            ServiceLocator.settingsStore.settings
                .map { it.toAnimationDecision() }
                .distinctUntilChanged()
                .collect { animated -> allowAnimation.value = animated }
        }
        scope.launch {
            // The overlay disappears the moment the user turns it off.
            ServiceLocator.settingsStore.settings
                .map { it.overlayEnabled }
                .distinctUntilChanged()
                .collect { enabled -> if (!enabled) stopSelf() }
        }
    }

    private fun UserSettings.toAnimationDecision(): Boolean =
        companionMode == CompanionMode.ALWAYS_REACTIVE &&
            proactiveAllowed() &&
            motionLevel != MotionLevel.OFF

    private fun addOverlay() {
        val view = ComposeView(this).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )
            setViewTreeLifecycleOwner(this@CompanionOverlayService)
            setViewTreeViewModelStoreOwner(this@CompanionOverlayService)
            setViewTreeSavedStateRegistryOwner(this@CompanionOverlayService)
            setContent {
                val animated by allowAnimation
                FanLensTheme {
                    OverlayBall(
                        animated = animated,
                        onOpen = { openPanel() },
                        onDismiss = { dismissOverlay() },
                    )
                }
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 160
        }
        attachDragListener(view, layoutParams)
        windowManager.addView(view, layoutParams)
        overlayView = view
    }

    /** Tap opens the panel; a tap refreshes data in On Open mode and can ask. */
    private fun openPanel() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** In-overlay dismiss: persists overlay-off so the ball stays hidden. */
    private fun dismissOverlay() {
        scope.launch {
            runCatching { ServiceLocator.settingsStore.setOverlayEnabled(false) }
        }
    }

    override fun onDestroy() {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun attachDragListener(view: View, layoutParams: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - touchX).roundToInt()
                    layoutParams.y = initialY + (event.rawY - touchY).roundToInt()
                    windowManager.updateViewLayout(view, layoutParams)
                    true
                }
                // A drag (not a tap) returns true so the click does not fire.
                MotionEvent.ACTION_UP -> abs(event.rawX - touchX) > 8 || abs(event.rawY - touchY) > 8
                else -> false
            }
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "companion_overlay"
        private const val NOTIFICATION_ID = 4201
    }
}
