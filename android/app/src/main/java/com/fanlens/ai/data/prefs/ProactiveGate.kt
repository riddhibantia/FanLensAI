package com.fanlens.ai.data.prefs

import kotlin.math.abs

/**
 * Pure decision logic for proactive mascot/AI behavior (TRD §4).
 * Everything a proactive reaction must satisfy before it may show:
 * mode + overlay + AI on + trigger opted in + outside quiet hours +
 * cooldown elapsed + gap threshold (for gap triggers).
 *
 * Kept free of Android framework types so it runs in plain JVM unit tests.
 *
 * This is the single gate. `UserSettings.proactiveAllowed()` delegates here so
 * the overlay cannot drift from the behavior these tests describe.
 */
object ProactiveGate {
    const val COOLDOWN_MILLIS: Long = 5 * 60 * 1000L

    fun canShowProactive(
        settings: UserSettings,
        trigger: TriggerCategory,
        gapDelta: Int? = null,
        lastShownAtMillis: Long? = null,
        nowMillis: Long = System.currentTimeMillis(),
        minuteOfDay: Int = currentMinuteOfDay(),
    ): Boolean {
        if (settings.companionMode != CompanionMode.ALWAYS_REACTIVE) return false
        if (!settings.overlayEnabled) return false
        if (!settings.aiEnabled) return false
        if (trigger !in settings.triggerCategories) return false
        if (settings.quietHours.covers(minuteOfDay)) return false
        // Notification opt-in is deliberately NOT checked here. Notification
        // categories control notifications; they must not silently disable
        // mascot motion. Requiring "INSIGHTS" here meant ALWAYS_REACTIVE did
        // nothing by default, since the default categories are {"SCORES"}.
        if (lastShownAtMillis != null) {
            // Clamp: a backward clock jump would otherwise make this negative
            // and block every reaction until wall-clock time caught up.
            val elapsed = nowMillis - lastShownAtMillis
            if (elapsed in 0 until COOLDOWN_MILLIS) return false
        }
        if (trigger == TriggerCategory.GAP_THRESHOLD) {
            // A missing delta means "unknown gap", so the threshold cannot be
            // shown to be met. Previously a null delta fell through to true,
            // firing the reaction regardless of the user's threshold.
            if (gapDelta == null || abs(gapDelta) < settings.gapThreshold) return false
        }
        return true
    }

    fun shouldRefresh(
        policy: RefreshPolicy,
        force: Boolean,
        onOpen: Boolean,
    ): Boolean {
        if (force) return true
        if (policy == RefreshPolicy.AUTO) return true
        if (policy == RefreshPolicy.ON_OPEN) return onOpen
        return false
    }

    private fun currentMinuteOfDay(): Int {
        val cal = java.util.Calendar.getInstance()
        return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
            cal.get(java.util.Calendar.MINUTE)
    }
}
