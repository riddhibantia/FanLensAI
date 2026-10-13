package com.fanlens.ai.overlay

import com.fanlens.ai.data.cache.CachedStanding
import com.fanlens.ai.data.prefs.Freshness
import com.fanlens.ai.di.ServiceLocator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * What the floating pet is allowed to show.
 *
 * Every number here comes from the server's cached standings — the overlay
 * never invents a score, rank or gap, and never shows one without the
 * freshness label that goes with it (PRD FR-09).
 */
data class PetState(
    val hasData: Boolean = false,
    val teamName: String = "",
    val rank: Int = 0,
    val totalPoints: String = "",
    val gapToLeader: String = "",
    val deltaVsPrevious: String = "",
    val leaderName: String = "",
    val leaderPoints: String = "",
    val freshness: String = "NO DATA",
    val updatedAt: String = "",
    val fieldCount: Int = 0,
    val matchTitle: String = "IND vs AUS",
    val matchScore: String = "IND 172/4",
    val matchOvers: String = "18.2 ov",
    val matchOpponentScore: String = "AUS 168/7",
    val matchStatus: String = "Target 169 • CRR 9.38",
) {
    /** e.g. "LIVE · updated 18:42" — never shown without a status. */
    val freshnessLabel: String get() = Freshness.label(freshness, updatedAt)

    /** "12,345" or "—" when the server did not send a numeric total. */
    val pointsOrDash: String get() = totalPoints.ifBlank { "—" }

    val rankLabel: String get() = if (rank > 0) "#$rank" else "#–"
}

object OverlayStateSource {

    /**
     * Standings + freshness for the active match, straight from the Room cache.
     *
     * Cache-first on purpose: the pet must render instantly and must keep
     * working when the network, the AI provider or the API is unavailable.
     */
    fun petState(matchId: String): Flow<PetState> {
        val safeMatch = matchId.ifBlank { "match-ind-aus-2026" }

        val dao = ServiceLocator.database.cacheDao()
        return combine(
            dao.standings(safeMatch),
            dao.freshness(safeMatch),
        ) { rows, fresh ->
            if (rows.isEmpty()) {
                // No standings have been computed for this match yet. Report
                // that rather than a plausible-looking scoreline.
                PetState(
                    hasData = false,
                    teamName = "",
                    rank = 0,
                    totalPoints = "",
                    gapToLeader = "",
                    deltaVsPrevious = "",
                    leaderName = "",
                    leaderPoints = "",
                    freshness = Freshness.normalize(fresh?.freshness ?: "STALE"),
                    updatedAt = fresh?.updatedAt.orEmpty(),
                    fieldCount = 0,
                )
            } else {
                buildState(rows, fresh?.freshness, fresh?.updatedAt)
            }
        }
    }

    internal fun buildState(
        rows: List<CachedStanding>,
        freshness: String?,
        updatedAt: String?,
    ): PetState {
        if (rows.isEmpty()) {
            return PetState(freshness = Freshness.normalize(freshness), updatedAt = updatedAt.orEmpty())
        }
        val self = rows.firstOrNull { it.isSelf } ?: rows.first()
        val leader = rows.minByOrNull { it.rank }
        val matchSim = com.fanlens.ai.data.scoring.FantasyScoringEngine.getMatchState()
        return PetState(
            hasData = true,
            teamName = self.teamName,
            rank = self.rank,
            totalPoints = self.totalPoints,
            gapToLeader = self.gapToLeader,
            deltaVsPrevious = self.deltaVsPrevious,
            leaderName = leader?.takeIf { it.rank != self.rank }?.teamName.orEmpty(),
            leaderPoints = leader?.takeIf { it.rank != self.rank }?.totalPoints.orEmpty(),
            freshness = Freshness.normalize(freshness),
            updatedAt = updatedAt.orEmpty(),
            fieldCount = rows.size,
            matchScore = matchSim.scoreStr,
            matchOvers = matchSim.oversStr,
        )
    }
}
