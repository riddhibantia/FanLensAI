package com.fanlens.ai.overlay

import com.fanlens.ai.data.cache.CachedStanding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pet must never show a number the server did not send, and never show a
 * score without its freshness. These cover the mapping from cached standings
 * to what the bubble renders.
 */
class OverlayStateTest {

    private fun standing(
        rank: Int,
        team: String,
        points: String,
        isSelf: Boolean = false,
        gap: String = "",
        delta: String = "",
    ) = CachedStanding(
        id = "$rank-$team",
        matchId = "m1",
        leagueId = "l1",
        rank = rank,
        fantasyTeamId = "ft-$team",
        userId = "u-$team",
        teamName = team,
        totalPoints = points,
        gapToLeader = gap,
        deltaVsPrevious = delta,
        isSelf = isSelf,
    )

    @Test
    fun emptyCache_reportsNoData() {
        val state = OverlayStateSource.buildState(emptyList(), "LIVE", "18:42")

        assertFalse(state.hasData)
        assertEquals("—", state.pointsOrDash)
        assertEquals("#–", state.rankLabel)
        assertEquals("LIVE · updated 18:42", state.freshnessLabel)
    }

    @Test
    fun selfRow_drivesTheCollapsedPill() {
        val state = OverlayStateSource.buildState(
            rows = listOf(
                standing(1, "Aarav", "412"),
                standing(2, "Arjun", "428", isSelf = true, gap = "16", delta = "+16"),
            ),
            freshness = "LIVE",
            updatedAt = "18:42",
        )

        assertTrue(state.hasData)
        assertEquals("Arjun", state.teamName)
        assertEquals(2, state.rank)
        assertEquals("#2", state.rankLabel)
        assertEquals("428", state.pointsOrDash)
        assertEquals("16", state.gapToLeader)
        assertEquals("+16", state.deltaVsPrevious)
    }

    @Test
    fun leaderIsTheLowestRankNotTheFirstRow() {
        val state = OverlayStateSource.buildState(
            rows = listOf(
                standing(2, "Arjun", "428", isSelf = true),
                standing(1, "Aarav", "444"),
            ),
            freshness = "LIVE",
            updatedAt = "18:42",
        )

        assertEquals("Aarav", state.leaderName)
        assertEquals("444", state.leaderPoints)
        assertEquals(2, state.fieldCount)
    }

    @Test
    fun whenYouLead_thereIsNobodyToChase() {
        val state = OverlayStateSource.buildState(
            rows = listOf(
                standing(1, "Arjun", "428", isSelf = true),
                standing(2, "Aarav", "412"),
            ),
            freshness = "LIVE",
            updatedAt = "18:42",
        )

        assertEquals("", state.leaderName)
        assertEquals("", state.leaderPoints)
    }

    @Test
    fun missingFreshness_neverBorrowsAStatus() {
        val state = OverlayStateSource.buildState(
            rows = listOf(standing(1, "Arjun", "428", isSelf = true)),
            freshness = null,
            updatedAt = null,
        )

        assertEquals("NO DATA", state.freshness)
        assertEquals("NO DATA · no timestamp yet", state.freshnessLabel)
    }

    @Test
    fun unknownFreshness_isNormalizedRatherThanShownRaw() {
        val state = OverlayStateSource.buildState(
            rows = listOf(standing(1, "Arjun", "428", isSelf = true)),
            freshness = "whatever-the-server-said",
            updatedAt = "18:42",
        )

        assertEquals("NO DATA", state.freshness)
    }

    @Test
    fun noSelfRow_fallsBackToTheTopRowRatherThanBlank() {
        val state = OverlayStateSource.buildState(
            rows = listOf(standing(1, "Aarav", "412")),
            freshness = "DEMO",
            updatedAt = "18:42",
        )

        assertTrue(state.hasData)
        assertEquals("Aarav", state.teamName)
    }
}
