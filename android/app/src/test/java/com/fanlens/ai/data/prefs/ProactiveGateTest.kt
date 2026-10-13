package com.fanlens.ai.data.prefs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProactiveGateTest {

    private fun reactiveSettings() = UserSettings(
        companionMode = CompanionMode.ALWAYS_REACTIVE,
        overlayEnabled = true,
        aiEnabled = true,
        triggerCategories = setOf(
            TriggerCategory.RANK_SWAP,
            TriggerCategory.GAP_THRESHOLD,
            TriggerCategory.MATCH_END,
        ),
        gapThreshold = 25,
        quietHours = QuietHours(enabled = false),
        notificationCategories = setOf("INSIGHTS"),
    )

    @Test
    fun tapToActivate_neverProactive() {
        val settings = reactiveSettings().copy(companionMode = CompanionMode.TAP_TO_ACTIVATE)
        assertFalse(
            ProactiveGate.canShowProactive(settings, TriggerCategory.RANK_SWAP, minuteOfDay = 600),
        )
    }

    @Test
    fun overlayOff_neverProactive() {
        val settings = reactiveSettings().copy(overlayEnabled = false)
        assertFalse(
            ProactiveGate.canShowProactive(settings, TriggerCategory.RANK_SWAP, minuteOfDay = 600),
        )
    }

    @Test
    fun aiOff_neverProactive() {
        val settings = reactiveSettings().copy(aiEnabled = false)
        assertFalse(
            ProactiveGate.canShowProactive(settings, TriggerCategory.RANK_SWAP, minuteOfDay = 600),
        )
    }

    @Test
    fun unsubscribedTrigger_neverProactive() {
        val settings = reactiveSettings().copy(triggerCategories = setOf(TriggerCategory.MATCH_END))
        assertFalse(
            ProactiveGate.canShowProactive(settings, TriggerCategory.RANK_SWAP, minuteOfDay = 600),
        )
    }

    @Test
    fun quietHours_suppressesProactive() {
        val settings = reactiveSettings().copy(
            quietHours = QuietHours(enabled = true, startMinutes = 22 * 60, endMinutes = 7 * 60),
        )
        assertFalse(
            ProactiveGate.canShowProactive(
                settings, TriggerCategory.RANK_SWAP, minuteOfDay = 23 * 60,
            ),
        )
        assertTrue(
            ProactiveGate.canShowProactive(
                settings, TriggerCategory.RANK_SWAP, minuteOfDay = 12 * 60,
            ),
        )
    }

    @Test
    fun cooldown_suppressesRepeatInsight() {
        val settings = reactiveSettings()
        val now = 1_000_000L
        assertFalse(
            ProactiveGate.canShowProactive(
                settings, TriggerCategory.RANK_SWAP,
                lastShownAtMillis = now - 60_000L, nowMillis = now, minuteOfDay = 600,
            ),
        )
        assertTrue(
            ProactiveGate.canShowProactive(
                settings, TriggerCategory.RANK_SWAP,
                lastShownAtMillis = now - ProactiveGate.COOLDOWN_MILLIS - 1, nowMillis = now,
                minuteOfDay = 600,
            ),
        )
    }

    @Test
    fun gapThreshold_enforced() {
        val settings = reactiveSettings()
        assertFalse(
            ProactiveGate.canShowProactive(
                settings, TriggerCategory.GAP_THRESHOLD, gapDelta = 10, minuteOfDay = 600,
            ),
        )
        assertTrue(
            ProactiveGate.canShowProactive(
                settings, TriggerCategory.GAP_THRESHOLD, gapDelta = 30, minuteOfDay = 600,
            ),
        )
    }

    @Test
    fun notificationOptOut_doesNotDisableMascotMotion() {
        // Notification categories control notifications, not mascot motion.
        // Tying them together meant ALWAYS_REACTIVE did nothing by default,
        // because the default categories are {"SCORES"}.
        val settings = reactiveSettings().copy(notificationCategories = setOf("SCORES"))
        assertTrue(
            ProactiveGate.canShowProactive(settings, TriggerCategory.RANK_SWAP, minuteOfDay = 600),
        )
    }

    @Test
    fun gapTriggerWithoutDelta_doesNotFire() {
        // A null delta means the gap is unknown, so the user's threshold cannot
        // be shown to be met. This used to fall through and fire regardless.
        val settings = reactiveSettings()
        assertFalse(
            ProactiveGate.canShowProactive(
                settings, TriggerCategory.GAP_THRESHOLD, gapDelta = null, minuteOfDay = 600,
            ),
        )
    }

    @Test
    fun backwardClock_doesNotBlockForever() {
        val settings = reactiveSettings()
        val now = 1_000_000L
        // lastShown is "in the future" because the clock moved back.
        assertTrue(
            ProactiveGate.canShowProactive(
                settings, TriggerCategory.RANK_SWAP,
                lastShownAtMillis = now + 60_000L, nowMillis = now, minuteOfDay = 600,
            ),
        )
    }

    @Test
    fun refreshPolicy_manualNeverAuto() {
        assertFalse(ProactiveGate.shouldRefresh(RefreshPolicy.MANUAL, force = false, onOpen = true))
        assertTrue(ProactiveGate.shouldRefresh(RefreshPolicy.MANUAL, force = true, onOpen = false))
        assertTrue(ProactiveGate.shouldRefresh(RefreshPolicy.AUTO, force = false, onOpen = false))
        assertTrue(ProactiveGate.shouldRefresh(RefreshPolicy.ON_OPEN, force = false, onOpen = true))
        assertFalse(ProactiveGate.shouldRefresh(RefreshPolicy.ON_OPEN, force = false, onOpen = false))
    }
}
