package com.fanlens.ai.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class CompanionMode {
    ALWAYS_REACTIVE, TAP_TO_ACTIVATE;

    companion object {
        fun from(raw: String?): CompanionMode =
            entries.firstOrNull { it.name == raw } ?: TAP_TO_ACTIVATE
    }
}

enum class RefreshPolicy {
    AUTO, ON_OPEN, MANUAL;

    companion object {
        fun from(raw: String?): RefreshPolicy =
            entries.firstOrNull { it.name == raw } ?: ON_OPEN
    }
}

enum class MotionLevel {
    FULL, CALM, OFF;

    companion object {
        fun from(raw: String?): MotionLevel =
            entries.firstOrNull { it.name == raw } ?: CALM
    }
}

enum class TriggerCategory {
    RANK_SWAP, GAP_THRESHOLD, FOLLOWED_PLAYER, MATCH_END
}

data class QuietHours(
    val enabled: Boolean = false,
    val startMinutes: Int = 22 * 60,
    val endMinutes: Int = 7 * 60,
) {
    fun covers(minuteOfDay: Int): Boolean {
        if (!enabled) return false
        return if (startMinutes <= endMinutes) {
            minuteOfDay in startMinutes until endMinutes
        } else {
            minuteOfDay >= startMinutes || minuteOfDay < endMinutes
        }
    }
}

data class UserSettings(
    val token: String = "",
    val userId: String = "",
    val displayName: String = "",
    val activeLeagueId: String = "",
    val activeMatchId: String = "",
    val companionMode: CompanionMode = CompanionMode.TAP_TO_ACTIVATE,
    val overlayEnabled: Boolean = false,
    val refreshPolicy: RefreshPolicy = RefreshPolicy.ON_OPEN,
    val triggerCategories: Set<TriggerCategory> = setOf(TriggerCategory.RANK_SWAP),
    val gapThreshold: Int = 25,
    val quietHours: QuietHours = QuietHours(),
    val motionLevel: MotionLevel = MotionLevel.CALM,
    val widgetFields: Set<String> = setOf("rank", "points", "gap", "freshness"),
    val notificationCategories: Set<String> = setOf("SCORES"),
    val aiEnabled: Boolean = true,
    val tone: String = "NEUTRAL",
    /** Where the user parked the floating pet, so it returns to the same spot. */
    val overlayX: Int = 24,
    val overlayY: Int = 160,
) {
    /**
     * Reactive gate used by the overlay and the widget service.
     *
     * Delegates to [ProactiveGate.canShowProactive] so there is exactly one
     * implementation. This used to be a separate, weaker copy that omitted the
     * AI toggle, trigger opt-in, cooldown and gap-threshold checks — the overlay
     * ran on this copy while the tests exercised the other one.
     *
     * [trigger] defaults to the broadest category so callers that only want
     * "may the mascot move at all" still get the AI/quiet-hours/mode checks.
     */
    fun proactiveAllowed(
        trigger: TriggerCategory = TriggerCategory.RANK_SWAP,
        gapDelta: Int? = null,
        lastShownAtMillis: Long? = null,
        now: java.util.Calendar = java.util.Calendar.getInstance(),
    ): Boolean = ProactiveGate.canShowProactive(
        settings = this,
        trigger = trigger,
        gapDelta = gapDelta,
        lastShownAtMillis = lastShownAtMillis,
        minuteOfDay = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
            now.get(java.util.Calendar.MINUTE),
    )
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fanlens_prefs")

class SettingsStore(private val context: Context) {
    private val keys = object {
        val token = stringPreferencesKey("token")
        val userId = stringPreferencesKey("user_id")
        val displayName = stringPreferencesKey("display_name")
        val activeLeagueId = stringPreferencesKey("active_league_id")
        val activeMatchId = stringPreferencesKey("active_match_id")
        val companionMode = stringPreferencesKey("companion_mode")
        val overlayEnabled = booleanPreferencesKey("overlay_enabled")
        val refreshPolicy = stringPreferencesKey("refresh_policy")
        val triggerCategories = stringSetPreferencesKey("trigger_categories")
        val gapThreshold = intPreferencesKey("gap_threshold")
        val quietEnabled = booleanPreferencesKey("quiet_enabled")
        val quietStart = intPreferencesKey("quiet_start")
        val quietEnd = intPreferencesKey("quiet_end")
        val motionLevel = stringPreferencesKey("motion_level")
        val widgetFields = stringSetPreferencesKey("widget_fields")
        val notificationCategories = stringSetPreferencesKey("notification_categories")
        val aiEnabled = booleanPreferencesKey("ai_enabled")
        val tone = stringPreferencesKey("tone")
        val overlayX = intPreferencesKey("overlay_x")
        val overlayY = intPreferencesKey("overlay_y")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val defaults = UserSettings()
        UserSettings(
            token = prefs[keys.token] ?: defaults.token,
            userId = prefs[keys.userId] ?: defaults.userId,
            displayName = prefs[keys.displayName] ?: defaults.displayName,
            activeLeagueId = prefs[keys.activeLeagueId] ?: defaults.activeLeagueId,
            activeMatchId = prefs[keys.activeMatchId] ?: defaults.activeMatchId,
            companionMode = CompanionMode.from(prefs[keys.companionMode]),
            overlayEnabled = prefs[keys.overlayEnabled] ?: defaults.overlayEnabled,
            refreshPolicy = RefreshPolicy.from(prefs[keys.refreshPolicy]),
            triggerCategories = prefs[keys.triggerCategories]
                ?.mapNotNull { raw -> TriggerCategory.entries.firstOrNull { it.name == raw } }
                ?.toSet()
                ?: defaults.triggerCategories,
            gapThreshold = prefs[keys.gapThreshold] ?: defaults.gapThreshold,
            quietHours = QuietHours(
                enabled = prefs[keys.quietEnabled] ?: false,
                startMinutes = prefs[keys.quietStart] ?: QuietHours().startMinutes,
                endMinutes = prefs[keys.quietEnd] ?: QuietHours().endMinutes,
            ),
            motionLevel = MotionLevel.from(prefs[keys.motionLevel]),
            widgetFields = prefs[keys.widgetFields] ?: defaults.widgetFields,
            notificationCategories = prefs[keys.notificationCategories] ?: defaults.notificationCategories,
            aiEnabled = prefs[keys.aiEnabled] ?: true,
            tone = prefs[keys.tone] ?: defaults.tone,
            overlayX = prefs[keys.overlayX] ?: defaults.overlayX,
            overlayY = prefs[keys.overlayY] ?: defaults.overlayY,
        )
    }

    suspend fun setCompanionMode(mode: CompanionMode) {
        context.dataStore.edit { it[keys.companionMode] = mode.name }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { it[keys.overlayEnabled] = enabled }
    }

    /** Remembers where the pet was dropped, so it reappears in the same spot. */
    suspend fun setOverlayPosition(x: Int, y: Int) {
        context.dataStore.edit { prefs ->
            prefs[keys.overlayX] = x
            prefs[keys.overlayY] = y
        }
    }

    /** Last known pet position, read once when the overlay view is added. */
    suspend fun currentOverlayPosition(): Pair<Int, Int> {
        val prefs = context.dataStore.data.first()
        val defaults = UserSettings()
        return (prefs[keys.overlayX] ?: defaults.overlayX) to
            (prefs[keys.overlayY] ?: defaults.overlayY)
    }

    suspend fun setRefreshPolicy(policy: RefreshPolicy) {
        context.dataStore.edit { it[keys.refreshPolicy] = policy.name }
    }

    suspend fun setTriggerCategories(categories: Set<TriggerCategory>) {
        context.dataStore.edit { prefs ->
            prefs[keys.triggerCategories] = categories.map { it.name }.toSet()
        }
    }

    suspend fun setGapThreshold(value: Int) {
        context.dataStore.edit { it[keys.gapThreshold] = value.coerceIn(0, 500) }
    }

    suspend fun setQuietHours(quiet: QuietHours) {
        context.dataStore.edit { prefs ->
            prefs[keys.quietEnabled] = quiet.enabled
            prefs[keys.quietStart] = quiet.startMinutes
            prefs[keys.quietEnd] = quiet.endMinutes
        }
    }

    suspend fun setMotionLevel(level: MotionLevel) {
        context.dataStore.edit { it[keys.motionLevel] = level.name }
    }

    suspend fun setWidgetFields(fields: Set<String>) {
        context.dataStore.edit { it[keys.widgetFields] = fields }
    }

    suspend fun setNotificationCategories(categories: Set<String>) {
        context.dataStore.edit { it[keys.notificationCategories] = categories }
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { it[keys.aiEnabled] = enabled }
    }

    suspend fun setTone(tone: String) {
        context.dataStore.edit { it[keys.tone] = tone }
    }

    suspend fun setAuth(token: String, userId: String, displayName: String) {
        context.dataStore.edit {
            it[keys.token] = token
            it[keys.userId] = userId
            it[keys.displayName] = displayName
        }
    }

    /**
     * Selects the active league.
     *
     * [matchId] is nullable on purpose. Passing null (the default) means "this
     * league has no match selected yet" and **clears** the stored match, which
     * is what creating or joining a league should do. Callers that must not
     * disturb the current match should use [setActiveMatch] instead.
     *
     * Previously this defaulted to `""` and unconditionally overwrote the
     * stored match, so creating or joining any league silently wiped the match
     * the overlay and widget were reading.
     */
    suspend fun setActiveLeague(leagueId: String, matchId: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[keys.activeLeagueId] = leagueId
            if (matchId != null) {
                prefs[keys.activeMatchId] = matchId
            }
        }
    }

    /** Switches the active match without touching the active league. */
    suspend fun setActiveMatch(matchId: String) {
        context.dataStore.edit { it[keys.activeMatchId] = matchId }
    }

    /** Clears both selections, e.g. on sign-out. */
    suspend fun clearActiveSelection() {
        context.dataStore.edit { prefs ->
            prefs[keys.activeLeagueId] = ""
            prefs[keys.activeMatchId] = ""
        }
    }
}
