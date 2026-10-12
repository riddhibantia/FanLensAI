package com.fanlens.ai.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLogicTest {

    @Test
    fun quietHours_coversOvernightWindow() {
        val quiet = QuietHours(enabled = true, startMinutes = 22 * 60, endMinutes = 7 * 60)
        assertTrue(quiet.covers(23 * 60))
        assertTrue(quiet.covers(3 * 60))
        assertFalse(quiet.covers(12 * 60))
    }

    @Test
    fun quietHours_disabledNeverCovers() {
        val quiet = QuietHours(enabled = false)
        assertFalse(quiet.covers(23 * 60))
    }

    @Test
    fun modeDefaults_toTapToActivate() {
        assertEquals(CompanionMode.TAP_TO_ACTIVATE, UserSettings().companionMode)
        assertEquals(CompanionMode.TAP_TO_ACTIVATE, CompanionMode.from(null))
        assertEquals(CompanionMode.TAP_TO_ACTIVATE, CompanionMode.from("BOGUS"))
    }

    @Test
    fun tapMode_proactiveAllowedIsFalse() {
        val settings = UserSettings(
            companionMode = CompanionMode.TAP_TO_ACTIVATE,
            overlayEnabled = true,
        )
        assertFalse(settings.proactiveAllowed())
    }

    @Test
    fun freshness_normalizesUnknown() {
        assertEquals("LIVE", Freshness.normalize("live"))
        assertEquals("DEMO", Freshness.normalize("demo"))
        assertEquals("NO DATA", Freshness.normalize(null))
        assertEquals("NO DATA", Freshness.normalize("BOGUS"))
    }

    @Test
    fun freshness_labelAlwaysHasStatus() {
        assertEquals("LIVE · updated t1", Freshness.label("LIVE", "t1"))
        assertEquals("NO DATA · no timestamp yet", Freshness.label("", null))
    }
}
