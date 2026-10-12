package com.fanlens.ai.data.prefs

import kotlin.math.abs

/**
 * Pure decision logic for proactive mascot/AI behavior (TRD §4).
 * Everything a proactive reaction must satisfy before it may show:
 * mode + overlay + AI on + trigger opted in + outside quiet hours +
 * cooldown elapsed + gap threshold (for gap triggers) + notification opt-in.
 *
 * Kept free of Android framework types so it runs in plain JVM unit tests.
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
        if (!settings.notificationCategories.contains("INSIGHTS")) return false
        if (lastShownAtMillis != null && nowMillis - lastShownAtMillis < COOLDOWN_MILLIS) {
            return false
        }
        if (trigger == TriggerCategory.GAP_THRESHOLD && gapDelta != null) {
            if (abs(gapDelta) < settings.gapThreshold) return false
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
