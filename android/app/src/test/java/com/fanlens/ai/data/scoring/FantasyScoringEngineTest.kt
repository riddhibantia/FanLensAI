package com.fanlens.ai.data.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Googly AI must describe the standings the engine currently reports.
 *
 * The prompt and the fallback used to carry a hardcoded scoreline, so the
 * assistant could describe a match state that had already changed. These cover
 * the grounding helpers: every number offered to the model or the user has to
 * come from the engine.
 */
class FantasyScoringEngineTest {

    @Test
    fun `standings are ordered by points with the leader first`() {
        val standings = FantasyScoringEngine.computeStandings()

        assertTrue("expected teams", standings.isNotEmpty())
        assertEquals(1, standings.first().rank)
        assertEquals(0, standings.first().gapToLeader.toInt())

        val points = standings.map { it.totalPoints.toInt() }
        assertEquals(points.sortedDescending(), points)
    }

    @Test
    fun `exactly one team is marked as the users team`() {
        val standings = FantasyScoringEngine.computeStandings()
        assertEquals(1, standings.count { it.isSelf })
    }

    @Test
    fun `every team except the leader reports a gap to the leader`() {
        val standings = FantasyScoringEngine.computeStandings()
        val leader = standings.first().totalPoints.toInt()

        standings.drop(1).forEach { row ->
            assertEquals(
                "gap for ${row.teamName}",
                (leader - row.totalPoints.toInt()).toString(),
                row.gapToLeader,
            )
        }
    }

    @Test
    fun `roster points apply the captain and vice multipliers`() {
        val self = FantasyScoringEngine.computeStandings().first { it.isSelf }
        val roster = FantasyScoringEngine.getRoster(self.fantasyTeamId)

        assertTrue("expected picks", roster.isNotEmpty())
        roster.forEach { member ->
            assertEquals(
                "points for ${member.player.name}",
                (member.player.basePoints * member.multiplier).toInt(),
                member.fantasyPoints,
            )
        }
    }

    @Test
    fun `team total is the sum of its picks`() {
        val self = FantasyScoringEngine.computeStandings().first { it.isSelf }
        val roster = FantasyScoringEngine.getRoster(self.fantasyTeamId)

        assertEquals(
            roster.sumOf { it.fantasyPoints },
            self.totalPoints.toInt(),
        )
    }

    @Test
    fun `simulating a delivery advances the match clock`() {
        val before = FantasyScoringEngine.getMatchState()
        FantasyScoringEngine.simulateNextDelivery()
        val after = FantasyScoringEngine.getMatchState()

        val beforeBalls = before.oversCompleted * 6 + before.ballsInOver
        val afterBalls = after.oversCompleted * 6 + after.ballsInOver
        assertTrue("clock should advance", afterBalls > beforeBalls)
    }

    @Test
    fun `standings after a simulated delivery stay consistent`() {
        FantasyScoringEngine.simulateNextDelivery()
        val standings = FantasyScoringEngine.computeStandings()

        val leader = standings.first().totalPoints.toInt()
        standings.forEach { row ->
            assertEquals(
                (leader - row.totalPoints.toInt()).toString(),
                row.gapToLeader,
            )
        }
        assertEquals(
            standings.indices.map { it + 1 },
            standings.map { it.rank },
        )
    }

    @Test
    fun `the match summary never reports a negative score`() {
        val state = FantasyScoringEngine.getMatchState()
        assertTrue(state.runs >= 0)
        assertTrue(state.wickets in 0..10)
        assertTrue(state.ballsInOver in 0..5)
        assertFalse(state.matchTitle.isBlank())
    }
}
