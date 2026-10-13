package com.fanlens.ai.data.scoring

import com.fanlens.ai.data.cache.CachedStanding

data class CricketPlayer(
    val id: String,
    val name: String,
    val team: String, // "IND" or "AUS"
    val role: String, // "BAT", "BOWL", "AR", "WK"
    val basePoints: Int,
)

data class RosterMember(
    val player: CricketPlayer,
    val isCaptain: Boolean = false,
    val isViceCaptain: Boolean = false,
) {
    val multiplier: Double
        get() = when {
            isCaptain -> 2.0
            isViceCaptain -> 1.5
            else -> 1.0
        }

    val fantasyPoints: Int
        get() = (player.basePoints * multiplier).toInt()
}

data class TeamRosterState(
    val teamId: String,
    val userId: String,
    val teamName: String,
    val isSelf: Boolean,
    val members: List<RosterMember>,
) {
    val totalPoints: Int
        get() = members.sumOf { it.fantasyPoints }
}

data class MatchSimulationState(
    val matchTitle: String = "IND vs AUS",
    val runs: Int = 172,
    val wickets: Int = 4,
    val oversCompleted: Int = 18,
    val ballsInOver: Int = 2,
    val target: Int = 169,
    val recentBalls: List<String> = listOf("2", "6", "0", "4", "1", "•"),
) {
    val oversStr: String
        get() = "$oversCompleted.$ballsInOver ov"
    val scoreStr: String
        get() = "IND $runs/$wickets"
}

data class ScenarioProjection(
    val currentPoints: Int,
    val projectedPoints: Int,
    val pointsDelta: Int,
    val currentRank: Int,
    val projectedRank: Int,
    val currentGap: Int,
    val projectedGap: Int,
    val leaderName: String,
    val explanation: String,
)

object FantasyScoringEngine {

    // Available players in the match
    val ALL_PLAYERS = listOf(
        CricketPlayer("p-kohli", "Virat Kohli", "IND", "BAT", 94),
        CricketPlayer("p-bumrah", "Jasprit Bumrah", "IND", "BOWL", 75),
        CricketPlayer("p-pandya", "Hardik Pandya", "IND", "AR", 22),
        CricketPlayer("p-head", "Travis Head", "AUS", "BAT", 58),
        CricketPlayer("p-cummins", "Pat Cummins", "AUS", "BOWL", 45),
        CricketPlayer("p-maxwell", "Glenn Maxwell", "AUS", "AR", 26),
        CricketPlayer("p-rohit", "Rohit Sharma", "IND", "BAT", 38),
        CricketPlayer("p-starc", "Mitchell Starc", "AUS", "BOWL", 36),
        CricketPlayer("p-rahul", "KL Rahul", "IND", "WK", 30),
        CricketPlayer("p-smith", "Steve Smith", "AUS", "BAT", 30),
        CricketPlayer("p-hazlewood", "Josh Hazlewood", "AUS", "BOWL", 46),
        CricketPlayer("p-jadeja", "Ravindra Jadeja", "IND", "AR", 28),
    )

    private var currentMatch = MatchSimulationState()

    // 4 Leagues Rosters
    private var teamRosters: MutableMap<String, TeamRosterState> = mutableMapOf(
        "team-siddhi" to TeamRosterState(
            teamId = "team-siddhi",
            userId = "u-siddhi",
            teamName = "Siddhi XI",
            isSelf = false,
            members = listOf(
                RosterMember(ALL_PLAYERS[1], isCaptain = true), // Bumrah 75 * 2 = 150
                RosterMember(ALL_PLAYERS[3], isViceCaptain = true), // Head 58 * 1.5 = 87
                RosterMember(ALL_PLAYERS[4]), // Cummins 45 * 1 = 45
                RosterMember(ALL_PLAYERS[9]), // Smith 30 * 1 = 30
            ), // Total = 312
        ),
        "team-riddhi" to TeamRosterState(
            teamId = "team-riddhi",
            userId = "u-riddhi",
            teamName = "Riddhi XI",
            isSelf = true,
            members = listOf(
                RosterMember(ALL_PLAYERS[0], isCaptain = true), // Kohli 94 * 2 = 188
                RosterMember(ALL_PLAYERS[2], isViceCaptain = true), // Pandya 22 * 1.5 = 33
                RosterMember(ALL_PLAYERS[1]), // Bumrah 45 (base 45 before milestone) * 1 = 45
                RosterMember(ALL_PLAYERS[5]), // Maxwell 18 * 1 = 18
            ), // Total = 284
        ),
        "team-nikki" to TeamRosterState(
            teamId = "team-nikki",
            userId = "u-nikki",
            teamName = "Nikki XI",
            isSelf = false,
            members = listOf(
                RosterMember(ALL_PLAYERS[3], isCaptain = true), // Head 58 * 2 = 116
                RosterMember(ALL_PLAYERS[5], isViceCaptain = true), // Maxwell 32 * 1.5 = 48
                RosterMember(ALL_PLAYERS[10]), // Hazlewood 46 * 1 = 46
                RosterMember(ALL_PLAYERS[8]), // Rahul 30 * 1 = 30
            ), // Total = 240
        ),
        "team-nakshatra" to TeamRosterState(
            teamId = "team-nakshatra",
            userId = "u-nakshatra",
            teamName = "Nakshatra XI",
            isSelf = false,
            members = listOf(
                RosterMember(ALL_PLAYERS[4], isCaptain = true), // Cummins 45 * 2 = 90
                RosterMember(ALL_PLAYERS[7], isViceCaptain = true), // Starc 36 * 1.5 = 54
                RosterMember(ALL_PLAYERS[6]), // Rohit 38 * 1 = 38
                RosterMember(ALL_PLAYERS[11]), // Jadeja 28 * 1 = 28
            ), // Total = 210
        ),
    )

    private val deltasMap = mutableMapOf<String, Int>(
        "team-siddhi" to 25,
        "team-riddhi" to 12,
        "team-nikki" to 4,
        "team-nakshatra" to -8,
    )

    fun getMatchState(): MatchSimulationState = currentMatch

    fun getRoster(teamId: String): List<RosterMember> {
        return teamRosters[teamId]?.members ?: emptyList()
    }

    fun updateSelfRoster(newMembers: List<RosterMember>) {
        val current = teamRosters["team-riddhi"] ?: return
        teamRosters["team-riddhi"] = current.copy(members = newMembers)
    }

    /**
     * Deterministically computes rankings from current roster states.
     */
    fun computeStandings(): List<CachedStanding> {
        val sortedTeams = teamRosters.values.sortedByDescending { it.totalPoints }
        val leaderPoints = sortedTeams.firstOrNull()?.totalPoints ?: 0

        return sortedTeams.mapIndexed { index, team ->
            val rank = index + 1
            val gap = (leaderPoints - team.totalPoints).coerceAtLeast(0)
            val delta = deltasMap[team.teamId] ?: 0
            val deltaStr = if (delta >= 0) "+$delta" else "$delta"

            CachedStanding(
                id = "match-ind-aus-2026:${team.teamId}",
                leagueId = "league-rivals-101",
                matchId = "match-ind-aus-2026",
                fantasyTeamId = team.teamId,
                userId = team.userId,
                teamName = team.teamName,
                rank = rank,
                totalPoints = team.totalPoints.toString(),
                gapToLeader = gap.toString(),
                deltaVsPrevious = deltaStr,
                isSelf = team.isSelf,
            )
        }
    }

    /**
     * Simulates 1 real ball with deterministic scoring impact across all rosters.
     */
    fun simulateNextDelivery(): Pair<MatchSimulationState, List<CachedStanding>> {
        var balls = currentMatch.ballsInOver + 1
        var overs = currentMatch.oversCompleted
        if (balls >= 6) {
            balls = 0
            overs += 1
        }

        // Cycle through exciting delivery outcomes
        val ballCycle = (currentMatch.runs + balls) % 4
        val (runsScored, ballLabel, kohliDelta, bumrahDelta, desc) = when (ballCycle) {
            0 -> Tuple5(6, "6", 14, 0, "Kohli lofts Cummins for a monstrous SIX over deep mid-wicket! (+28 pts to Riddhi XI)")
            1 -> Tuple5(4, "4", 10, 0, "Kohli crunches a cover drive for FOUR! (+20 pts to Riddhi XI)")
            2 -> Tuple5(1, "1", 2, 0, "Pandya pushes to mid-off for a sharp single (+3 pts to Riddhi XI)")
            else -> Tuple5(0, "W", 0, 25, "Cummins strikes! Pandya caught at extra cover (+25 pts to Cummins owners)")
        }

        val updatedRecent = (listOf(ballLabel) + currentMatch.recentBalls).take(6)
        currentMatch = currentMatch.copy(
            runs = currentMatch.runs + runsScored,
            wickets = if (ballLabel == "W") currentMatch.wickets + 1 else currentMatch.wickets,
            oversCompleted = overs,
            ballsInOver = balls,
            recentBalls = updatedRecent,
        )

        // Apply fantasy scoring delta to team rosters
        teamRosters.forEach { (teamId, teamState) ->
            var teamDelta = 0
            val updatedMembers = teamState.members.map { member ->
                if (member.player.id == "p-kohli" && kohliDelta > 0) {
                    val newBase = member.player.basePoints + kohliDelta
                    teamDelta += (kohliDelta * member.multiplier).toInt()
                    member.copy(player = member.player.copy(basePoints = newBase))
                } else if (member.player.id == "p-bumrah" && bumrahDelta > 0) {
                    val newBase = member.player.basePoints + bumrahDelta
                    teamDelta += (bumrahDelta * member.multiplier).toInt()
                    member.copy(player = member.player.copy(basePoints = newBase))
                } else {
                    member
                }
            }
            teamRosters[teamId] = teamState.copy(members = updatedMembers)
            deltasMap[teamId] = teamDelta
        }

        return Pair(currentMatch, computeStandings())
    }

    /**
     * Projects a What-If Over scenario mathematically without committing it.
     */
    fun projectWhatIf(scenario: String, runs: Int, wickets: Int): ScenarioProjection {
        val selfTeam = teamRosters["team-riddhi"] ?: return fallbackProjection()
        val leaderTeam = teamRosters["team-siddhi"] ?: return fallbackProjection()

        val selfKohli = selfTeam.members.find { it.player.id == "p-kohli" }
        val leaderBumrah = leaderTeam.members.find { it.player.id == "p-bumrah" }

        val kohliMultiplier = selfKohli?.multiplier ?: 2.0
        val bumrahMultiplier = leaderBumrah?.multiplier ?: 2.0

        val (selfGained, leaderGained, explanation) = when (scenario) {
            "A" -> { // Wickets scenario
                val bowlPts = wickets * 25
                val leadPts = (bowlPts * bumrahMultiplier).toInt()
                val userPts = 0
                Triple(userPts, leadPts, "Siddhi XI has Bumrah as Captain ($bumrahMultiplier x), gaining +$leadPts pts on $wickets wickets.")
            }
            "B" -> { // Dot balls scenario
                val leadPts = 8
                val userPts = 2
                Triple(userPts, leadPts, "Tight maiden/dot balls award economy bonuses. Gap stays around ${leaderTeam.totalPoints - selfTeam.totalPoints} pts.")
            }
            else -> { // Boundaries scenario (Scenario C)
                val batPts = runs + (if (runs >= 12) 6 else 2) // boundary bonuses
                val userPts = (batPts * kohliMultiplier).toInt()
                val leadPts = 0
                Triple(userPts, leadPts, "Virat Kohli hits boundaries! As your Captain ($kohliMultiplier x), you gain +$userPts pts!")
            }
        }

        val projSelf = selfTeam.totalPoints + selfGained
        val projLeader = leaderTeam.totalPoints + leaderGained
        val projRank = if (projSelf >= projLeader) 1 else 2
        val projGap = (projLeader - projSelf).coerceAtLeast(0)

        return ScenarioProjection(
            currentPoints = selfTeam.totalPoints,
            projectedPoints = projSelf,
            pointsDelta = selfGained,
            currentRank = 2,
            projectedRank = projRank,
            currentGap = (leaderTeam.totalPoints - selfTeam.totalPoints).coerceAtLeast(0),
            projectedGap = projGap,
            leaderName = leaderTeam.teamName,
            explanation = explanation,
        )
    }

    private fun fallbackProjection(): ScenarioProjection {
        return ScenarioProjection(284, 312, 28, 2, 1, 28, 0, "Siddhi XI", "Kohli boundaries can cut the 28-pt deficit and tie for #1.")
    }

    private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
}
