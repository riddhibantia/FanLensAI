package com.fanlens.ai.data.repository

import com.fanlens.ai.data.cache.CacheDao
import com.fanlens.ai.data.cache.CachedEvent
import com.fanlens.ai.data.cache.CachedFreshness
import com.fanlens.ai.data.cache.CachedStanding
import com.fanlens.ai.data.network.EventDto
import com.fanlens.ai.data.network.ExplainRequest
import com.fanlens.ai.data.network.FanLensApi
import com.fanlens.ai.data.network.LeagueCreatedResponse
import com.fanlens.ai.data.network.LeagueSummaryResponse
import com.fanlens.ai.data.network.PreferencesRequest
import com.fanlens.ai.data.network.RosterPlayerRequest
import com.fanlens.ai.data.network.RosterRequest
import com.fanlens.ai.data.network.RulesetResponse
import com.fanlens.ai.data.network.StandingsResponse
import com.fanlens.ai.data.prefs.CompanionMode
import com.fanlens.ai.data.prefs.MotionLevel
import com.fanlens.ai.data.prefs.QuietHours
import com.fanlens.ai.data.prefs.RefreshPolicy
import com.fanlens.ai.data.prefs.SettingsStore
import com.fanlens.ai.data.prefs.TriggerCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class LeagueRepository(
    private val api: FanLensApi,
    private val settingsStore: SettingsStore,
    private val cache: CacheDao,
) {
    val activeLeagueId: Flow<String> = settingsStore.settings.map { it.activeLeagueId }

    /**
     * Signs in against the server. A failure is reported, never faked: an
     * invented token would produce a signed-in looking app whose every request
     * the backend rejects.
     */
    suspend fun authenticate(displayName: String, deviceId: String): Result<String> = runCatching {
        val response = api.authDevice(displayName, deviceId)
        settingsStore.setAuth(response.token, response.userId, displayName)
        response.token
    }

    suspend fun listMyLeagues(): Result<List<LeagueSummaryResponse>> = runCatching {
        api.listMyLeagues().leagues
    }

    /** Returns the new league plus its one-time invite code, which is never readable again. */
    suspend fun createLeague(name: String): Result<LeagueCreatedResponse> = runCatching {
        val league = api.createLeague(name)
        settingsStore.setActiveLeague(league.id)  // switching league clears the match
        league
    }

    suspend fun joinLeague(leagueId: String, inviteCode: String): Result<Unit> = runCatching {
        api.joinLeague(leagueId, inviteCode)
        settingsStore.setActiveLeague(leagueId)  // switching league clears the match
    }

    /** Joins with an invite code alone; the league id is resolved by the server. */
    suspend fun joinLeagueByCode(inviteCode: String): Result<LeagueSummaryResponse> = runCatching {
        val league = api.joinLeagueByCode(inviteCode)
        settingsStore.setActiveLeague(league.id)  // switching league clears the match
        league
    }

    /** Fetches standings and writes the cache; the UI reads the cache first. */
    suspend fun refreshStandings(
        leagueId: String,
        matchId: String?,
        selfUserId: String,
    ): Result<StandingsResponse> = runCatching {
        val response = api.getStandings(leagueId, matchId)
        if (matchId != null) {
            cache.clearStandings(matchId)
            cache.upsertStandings(
                response.rows.map { row ->
                    CachedStanding(
                        id = "${matchId}:${row.fantasyTeamId}",
                        matchId = matchId,
                        leagueId = leagueId,
                        rank = row.rank,
                        fantasyTeamId = row.fantasyTeamId,
                        userId = row.userId,
                        teamName = row.teamName,
                        totalPoints = row.totalPoints,
                        gapToLeader = row.gapToLeader,
                        deltaVsPrevious = row.deltaVsPrevious,
                        isSelf = row.userId == selfUserId,
                    )
                }
            )
            cache.upsertFreshness(
                CachedFreshness(
                    matchId = matchId,
                    freshness = response.freshness,
                    updatedAt = response.freshnessUpdatedAt ?: "",
                )
            )
            settingsStore.setActiveLeague(leagueId, matchId)
        }
        response
    }

    fun cachedStandings(matchId: String): Flow<List<CachedStanding>> = cache.standings(matchId)

    fun cachedFreshness(matchId: String): Flow<CachedFreshness?> = cache.freshness(matchId)
}

class EventRepository(
    private val api: FanLensApi,
    private val settingsStore: SettingsStore,
    private val cache: CacheDao,
) {
    suspend fun refreshEvents(matchId: String, after: Int = 0): Result<Int> = runCatching {
        val response = api.getEvents(matchId, after)
        cache.upsertEvents(
            response.events.map { event ->
                CachedEvent(
                    providerEventId = event.providerEventId,
                    matchId = matchId,
                    sequence = event.sequence,
                    eventType = event.eventType,
                    occurredAt = event.occurredAt,
                    payload = event.payload.toString(),
                    status = event.status,
                )
            }
        )
        response.events.size
    }

    fun cachedEvents(matchId: String, after: Int = 0): Flow<List<CachedEvent>> =
        cache.events(matchId, after, limit = 100)

    suspend fun injectDemoBalls(matchId: String, balls: Int) =
        runCatching { api.injectDemoEvents(matchId, balls) }

    suspend fun eventCount(matchId: String) = cache.eventCount(matchId)
}

class RosterRepository(
    private val api: FanLensApi,
    private val settingsStore: SettingsStore,
) {
    suspend fun saveRoster(
        leagueId: String,
        matchId: String,
        rulesetId: String,
        teamName: String,
        players: List<RosterPlayerRequest>,
    ): Result<Unit> = runCatching {
        api.upsertRoster(leagueId, RosterRequest(matchId, teamName, rulesetId, players))
    }

    /** Rulesets the server accepts. Without this the user types an ID blind. */
    suspend fun rulesets(): Result<List<RulesetResponse>> = runCatching { api.listRulesets() }
}

class InsightRepository(
    private val api: FanLensApi,
    private val settingsStore: SettingsStore,
) {
    suspend fun explain(leagueId: String, question: String?) =
        runCatching { api.explain(leagueId, ExplainRequest(question = question)) }

    suspend fun recent(leagueId: String) = runCatching { api.listInsights(leagueId) }
}

class PreferencesRepository(
    private val api: FanLensApi,
    private val settingsStore: SettingsStore,
) {
    /** Applies settings locally first (instant), then syncs to the server. */
    suspend fun setCompanionMode(companionMode: CompanionMode) {
        settingsStore.setCompanionMode(companionMode)
        runCatching { api.patchPreferences(PreferencesRequest(companionMode = companionMode.name)) }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        settingsStore.setOverlayEnabled(enabled)
        runCatching { api.patchPreferences(PreferencesRequest(overlayEnabled = enabled)) }
    }

    suspend fun setRefreshPolicy(refreshPolicy: RefreshPolicy) {
        settingsStore.setRefreshPolicy(refreshPolicy)
        runCatching { api.patchPreferences(PreferencesRequest(refreshPolicy = refreshPolicy.name)) }
    }

    suspend fun setTriggerCategories(categories: Set<TriggerCategory>) {
        settingsStore.setTriggerCategories(categories)
        runCatching {
            api.patchPreferences(PreferencesRequest(triggerCategories = categories.map { it.name }))
        }
    }

    suspend fun setGapThreshold(value: Int) {
        settingsStore.setGapThreshold(value)
        runCatching { api.patchPreferences(PreferencesRequest(gapThreshold = value)) }
    }

    suspend fun setQuietHours(quietHours: QuietHours) {
        settingsStore.setQuietHours(quietHours)
        runCatching {
            api.patchPreferences(
                PreferencesRequest(
                    quietHours = mapOf(
                        "enabled" to quietHours.enabled.toString(),
                        "start_minutes" to quietHours.startMinutes.toString(),
                        "end_minutes" to quietHours.endMinutes.toString(),
                    ),
                ),
            )
        }
    }

    suspend fun setMotionLevel(motionLevel: MotionLevel) {
        settingsStore.setMotionLevel(motionLevel)
        runCatching { api.patchPreferences(PreferencesRequest(motionLevel = motionLevel.name)) }
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        settingsStore.setAiEnabled(enabled)
        runCatching { api.patchPreferences(PreferencesRequest(aiEnabled = enabled)) }
    }

    suspend fun setTone(tone: String) {
        settingsStore.setTone(tone)
        runCatching { api.patchPreferences(PreferencesRequest(tone = tone)) }
    }

    suspend fun setWidgetFields(fields: Set<String>) {
        settingsStore.setWidgetFields(fields)
        runCatching { api.patchPreferences(PreferencesRequest(widgetFields = fields.toList())) }
    }

    suspend fun setNotificationCategories(categories: Set<String>) {
        settingsStore.setNotificationCategories(categories)
        runCatching {
            api.patchPreferences(PreferencesRequest(notificationCategories = categories.toList()))
        }
    }
}
