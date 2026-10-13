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
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
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
import com.fanlens.ai.ui.theme.FanLensTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The cross-app mascot. Best-effort by design: the service stops when the user
 * turns the overlay off, dismisses the ball, or revokes the permission.
 */
@kotlin.OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    /** Live standings for the pet. Read from the Room cache, never invented. */
    private val petState = MutableStateFlow(PetState())

    /** Which overlay fields the user chose to see, shared with the widget. */
    private val overlayFields = MutableStateFlow(UserSettings().widgetFields)

    /** Restored once in onCreate; the pet reappears where it was left. */
    private var savedPosition: Pair<Int, Int> = UserSettings().let { it.overlayX to it.overlayY }

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
        // Load the parked position off the main thread, then add the view once.
        scope.launch {
            savedPosition = runCatching {
                ServiceLocator.settingsStore.currentOverlayPosition()
            }.getOrElse { savedPosition }
            if (overlayView == null) addOverlay()
        }
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
        scope.launch {
            // Follow whichever match is active, and re-read the cache when it changes.
            ServiceLocator.settingsStore.settings
                .map { it.activeMatchId }
                .distinctUntilChanged()
                .flatMapLatest { matchId -> OverlayStateSource.petState(matchId) }
                .onEach { petState.value = it }
                .collect {}
        }
        scope.launch {
            ServiceLocator.settingsStore.settings
                .map { it.widgetFields }
                .distinctUntilChanged()
                .collect { overlayFields.value = it }
        }
    }

    /**
     * Whether the mascot may idle-animate.
     *
     * Uses the shared gate rather than a local copy, so the overlay obeys the
     * same AI toggle, quiet hours and mode rules the tests describe. TAP_TO_ACTIVATE
     * is checked both here and inside the gate (which subsumes the mode check).
     */
    private fun UserSettings.toAnimationDecision(): Boolean =
        companionMode == CompanionMode.ALWAYS_REACTIVE &&
            proactiveAllowed() &&
            motionLevel != MotionLevel.OFF

    private fun addOverlay() {
        // Wrap ComposeView in a FrameLayout so we can intercept touches at the
        // raw View level BEFORE Compose's gesture system sees them. Without this
        // wrapper, Compose consumed all touch events and drag never worked.
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )
            setViewTreeLifecycleOwner(this@CompanionOverlayService)
            setViewTreeViewModelStoreOwner(this@CompanionOverlayService)
            setViewTreeSavedStateRegistryOwner(this@CompanionOverlayService)
            setContent {
                val animated by allowAnimation
                val pet by collectAsComposeState(petState)
                val fields by collectAsComposeState(overlayFields)
                FanLensTheme {
                    OverlayBall(
                        state = pet,
                        animated = animated,
                        overlayFields = fields,
                        onOpen = { openPanel() },
                        onDismiss = { dismissOverlay() },
                        onLoadDemo = { loadDemoScores() },
                    )
                }
            }
        }

        val saved = savedPosition
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // NOT_FOCUSABLE: keyboard stays in the app beneath us
            // NOT_TOUCH_MODAL: taps outside our bounds go through to the app
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = saved.first
            y = saved.second
        }

        // DragFrameLayout sits between WindowManager and ComposeView.
        // It intercepts touch before Compose and handles the window move.
        val dragFrame = object : FrameLayout(this) {
            private var dragStartX = 0f
            private var dragStartY = 0f
            private var paramStartX = 0
            private var paramStartY = 0
            private var isDragging = false
            private val DRAG_THRESHOLD = 8f // px

            override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dragStartX = ev.rawX
                        dragStartY = ev.rawY
                        paramStartX = layoutParams.x
                        paramStartY = layoutParams.y
                        isDragging = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = abs(ev.rawX - dragStartX)
                        val dy = abs(ev.rawY - dragStartY)
                        if (!isDragging && (dx > DRAG_THRESHOLD || dy > DRAG_THRESHOLD)) {
                            isDragging = true
                        }
                        if (isDragging) {
                            layoutParams.x = paramStartX + (ev.rawX - dragStartX).roundToInt()
                            layoutParams.y = paramStartY + (ev.rawY - dragStartY).roundToInt()
                            runCatching { windowManager.updateViewLayout(this, layoutParams) }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            rememberPosition(layoutParams.x, layoutParams.y)
                        }
                        val wasDragging = isDragging
                        isDragging = false
                        // Intercept (consume) if it was a drag so clicks don't fire
                        return wasDragging
                    }
                }
                // Never intercept down/move so Compose still tracks the pointer
                return false
            }

            override fun onTouchEvent(ev: MotionEvent): Boolean {
                // Consume events that reach here during a drag
                return isDragging
            }
        }

        dragFrame.addView(composeView)
        dragFrame.setViewTreeLifecycleOwner(this)
        dragFrame.setViewTreeViewModelStoreOwner(this)
        dragFrame.setViewTreeSavedStateRegistryOwner(this)
        windowManager.addView(dragFrame, layoutParams)
        overlayView = dragFrame
    }

    /** Tap opens the panel; a tap refreshes data in On Open mode and can ask. */
    private fun openPanel() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Demo path for an empty pet (PRD FR-12).
     *
     * Injects scripted balls through the real backend endpoint, then refreshes
     * standings so the numbers the pet shows come from the server's scoring
     * engine and carry a DEMO freshness label. Nothing is faked on the device.
     */
    private fun loadDemoScores() {
        scope.launch {
            val settings = ServiceLocator.settingsStore.settings.first()
            val matchId = settings.activeMatchId
            val leagueId = settings.activeLeagueId
            if (matchId.isBlank() || leagueId.isBlank()) {
                // No match selected yet: the app has to seed/pick one first.
                openPanel()
                return@launch
            }
            runCatching {
                ServiceLocator.eventRepository.injectDemoBalls(matchId, balls = 5)
                ServiceLocator.leagueRepository.refreshStandings(
                    leagueId = leagueId,
                    matchId = matchId,
                    selfUserId = settings.userId,
                )
            }
        }
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



    /** The pet returns to wherever the user last dropped it. */
    private fun rememberPosition(x: Int, y: Int) {
        scope.launch {
            runCatching { ServiceLocator.settingsStore.setOverlayPosition(x, y) }
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

/**
 * Reads a StateFlow as Compose state. Top-level rather than a service member
 * because a Service is not a composable scope.
 */
@Composable
internal fun <T> collectAsComposeState(
    flow: kotlinx.coroutines.flow.StateFlow<T>,
): androidx.compose.runtime.State<T> {
    val state = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(flow.value)
    }
    androidx.compose.runtime.LaunchedEffect(flow) {
        flow.collect { state.value = it }
    }
    return state
}
