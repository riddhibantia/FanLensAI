package com.fanlens.ai.data.repository

import com.fanlens.ai.data.cache.CacheDao
import com.fanlens.ai.data.cache.CachedEvent
import com.fanlens.ai.data.cache.CachedFreshness
import com.fanlens.ai.data.cache.CachedStanding
import com.fanlens.ai.data.cache.MockDataSeeder
import com.fanlens.ai.data.network.FanLensApi
import com.fanlens.ai.data.network.InsightDto
import com.fanlens.ai.data.network.LeagueCreatedResponse
import com.fanlens.ai.data.network.LeagueSummaryResponse
import com.fanlens.ai.data.network.RosterPlayerRequest
import com.fanlens.ai.data.network.RulesetResponse
import com.fanlens.ai.data.network.StandingRowDto
import com.fanlens.ai.data.network.StandingsResponse
import com.fanlens.ai.data.prefs.CompanionMode
import com.fanlens.ai.data.prefs.MotionLevel
import com.fanlens.ai.data.prefs.QuietHours
import com.fanlens.ai.data.prefs.RefreshPolicy
import com.fanlens.ai.data.prefs.SettingsStore
import com.fanlens.ai.data.prefs.TriggerCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class LeagueRepository(
    private val api: FanLensApi,
    private val settingsStore: SettingsStore,
    private val cache: CacheDao,
) {
    val activeLeagueId: Flow<String> = settingsStore.settings.map { it.activeLeagueId }

    private val localLeagues = mutableListOf(
        LeagueSummaryResponse(
            id = "league-demo-1",
            name = "Sunday Smashers",
            ownerUserId = "u-riddhi",
            createdAt = "2026-10-10",
            isOwner = true,
            memberCount = 4,
            teamCount = 4,
        ),
        LeagueSummaryResponse(
            id = "league-demo-2",
            name = "Gully Gang 2026",
            ownerUserId = "u-siddhi",
            createdAt = "2026-10-11",
            isOwner = false,
            memberCount = 3,
            teamCount = 3,
        ),
    )

    /**
     * Signs in standalone without requiring an external backend.
     */
    suspend fun authenticate(displayName: String, deviceId: String): Result<String> = runCatching {
        val safeName = displayName.ifBlank { "Riddhi" }
        val token = "standalone-demo-$safeName"
        val userId = "u-${safeName.lowercase().replace(" ", "")}"
        settingsStore.setAuth(token, userId, safeName)
        MockDataSeeder.seedDefaults(cache)
        token
    }

    suspend fun listMyLeagues(): Result<List<LeagueSummaryResponse>> = runCatching {
        localLeagues.toList()
    }

    /** Returns the new circle with an invite code. */
    suspend fun createLeague(name: String): Result<LeagueCreatedResponse> = runCatching {
        val safeName = name.ifBlank { "Sunday Smashers" }
        val newId = "league-${System.currentTimeMillis() % 10000}"
        val code = "SMASH${(10..99).random()}"
        val response = LeagueCreatedResponse(
            id = newId,
            name = safeName,
            ownerUserId = "u-riddhi",
            createdAt = "2026-10-14",
            inviteCode = code,
            inviteExpiresAt = "2026-10-21",
        )
        localLeagues.add(
            LeagueSummaryResponse(
                id = newId,
                name = safeName,
                ownerUserId = "u-riddhi",
                createdAt = "2026-10-14",
                isOwner = true,
                memberCount = 1,
                teamCount = 1,
            )
        )
        settingsStore.setActiveLeague(newId, "match-ind-aus-2026")
        response
    }

    suspend fun joinLeague(leagueId: String, inviteCode: String): Result<Unit> = runCatching {
        settingsStore.setActiveLeague(leagueId, "match-ind-aus-2026")
    }

    suspend fun joinLeagueByCode(inviteCode: String): Result<LeagueSummaryResponse> = runCatching {
        val league = localLeagues.firstOrNull() ?: LeagueSummaryResponse(
            id = "league-demo-1",
            name = "Sunday Smashers",
            ownerUserId = "u-riddhi",
            createdAt = "2026-10-10",
            isOwner = true,
            memberCount = 4,
            teamCount = 4,
        )
        settingsStore.setActiveLeague(league.id, "match-ind-aus-2026")
        league
    }

    /** Fetches standings and ensures cache is populated. */
    suspend fun refreshStandings(
        leagueId: String,
        matchId: String?,
        selfUserId: String,
    ): Result<StandingsResponse> = runCatching {
        val safeMatch = matchId?.ifBlank { "match-ind-aus-2026" } ?: "match-ind-aus-2026"
        val safeLeague = leagueId.ifBlank { "league-demo-1" }

        val computed = com.fanlens.ai.data.scoring.FantasyScoringEngine.computeStandings()
        cache.clearStandings(safeMatch)
        cache.upsertStandings(computed)
        val matchState = com.fanlens.ai.data.scoring.FantasyScoringEngine.getMatchState()
        cache.upsertFreshness(
            CachedFreshness(
                matchId = safeMatch,
                freshness = "LIVE",
                updatedAt = "Over ${matchState.oversStr} • ${matchState.scoreStr}",
            )
        )
        settingsStore.setActiveLeague(safeLeague, safeMatch)

        StandingsResponse(
            matchId = safeMatch,
            freshness = "LIVE",
            freshnessUpdatedAt = "Over ${matchState.oversStr} • ${matchState.scoreStr}",
            rows = computed.map {
                StandingRowDto(
                    rank = it.rank,
                    fantasyTeamId = it.fantasyTeamId,
                    userId = it.userId,
                    teamName = it.teamName,
                    totalPoints = it.totalPoints,
                    gapToLeader = it.gapToLeader,
                    deltaVsPrevious = it.deltaVsPrevious,
                )
            },
        )
    }

    fun cachedStandings(matchId: String): Flow<List<CachedStanding>> {
        val safeMatch = matchId.ifBlank { "match-ind-aus-2026" }
        return cache.standings(safeMatch)
    }

    fun cachedFreshness(matchId: String): Flow<CachedFreshness?> {
        val safeMatch = matchId.ifBlank { "match-ind-aus-2026" }
        return cache.freshness(safeMatch)
    }
}

class EventRepository(
    private val api: FanLensApi,
    private val settingsStore: SettingsStore,
    private val cache: CacheDao,
) {
    private var simulatedBallCount = 0

    suspend fun refreshEvents(matchId: String, after: Int = 0): Result<Int> = runCatching {
        val safeMatch = matchId.ifBlank { "match-ind-aus-2026" }
        val count = cache.eventCount(safeMatch)
        if (count == 0) {
            MockDataSeeder.seedDefaults(cache)
        }
        cache.eventCount(safeMatch)
    }

    fun cachedEvents(matchId: String, after: Int = 0): Flow<List<CachedEvent>> {
        val safeMatch = matchId.ifBlank { "match-ind-aus-2026" }
        return cache.events(safeMatch, after, limit = 100)
    }

    suspend fun injectDemoBalls(matchId: String, balls: Int): Result<Unit> = runCatching {
        val safeMatch = matchId.ifBlank { "match-ind-aus-2026" }
        simulatedBallCount++

        val (matchState, updatedStandings) = com.fanlens.ai.data.scoring.FantasyScoringEngine.simulateNextDelivery()
        cache.clearStandings(safeMatch)
        cache.upsertStandings(updatedStandings)
        cache.upsertFreshness(
            CachedFreshness(
                matchId = safeMatch,
                freshness = "LIVE",
                updatedAt = "Over ${matchState.oversStr} • ${matchState.scoreStr}",
            )
        )

        val seq = 106 + simulatedBallCount
        val lastBall = matchState.recentBalls.firstOrNull() ?: "4"
        val newEvent = CachedEvent(
            providerEventId = "evt-$seq",
            matchId = safeMatch,
            sequence = seq,
            eventType = when (lastBall) {
                "6" -> "BOUNDARY_SIX"
                "4" -> "BOUNDARY_FOUR"
                "W" -> "WICKET"
                else -> "DELIVERY"
            },
            occurredAt = "18:45:${(10 * simulatedBallCount) % 60}",
            payload = """{"over":"${matchState.oversStr}","desc":"Ball delivery: $lastBall (${matchState.scoreStr})","impact":"Standings updated"}""",
            status = "OFFICIAL",
        )
        cache.upsertEvents(listOf(newEvent))
    }

    suspend fun eventCount(matchId: String) = cache.eventCount(matchId.ifBlank { "match-ind-aus-2026" })
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
    ): Result<Unit> = Result.success(Unit)

    suspend fun rulesets(): Result<List<RulesetResponse>> = Result.success(
        listOf(
            RulesetResponse(id = "ruleset-standard-t20", name = "Standard T20 Ruleset", version = "1.0.0")
        )
    )
}

class InsightRepository(
    private val api: FanLensApi,
    private val settingsStore: SettingsStore,
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Builds the system message from the same engine state the screens render.
     *
     * This used to be a hardcoded paragraph naming fixed scores, which meant the
     * model kept describing a match state that had already moved on. Every fact
     * below is read at request time.
     */
    private fun buildSystemPrompt(): String {
        val engine = com.fanlens.ai.data.scoring.FantasyScoringEngine
        val match = engine.getMatchState()
        val standings = engine.computeStandings()
        val self = standings.firstOrNull { it.isSelf }

        val table = standings.joinToString("\n") { row ->
            val who = if (row.isSelf) " (the user's team)" else ""
            "  #${row.rank} ${row.teamName}: ${row.totalPoints} pts$who"
        }

        val myPicks = self?.fantasyTeamId
            ?.let { engine.getRoster(it) }
            ?.sortedByDescending { it.fantasyPoints }
            ?.take(3)
            ?.joinToString(", ") { "${it.player.name} ${it.fantasyPoints} pts" }
            ?: "not available"

        val standingLine = if (self == null) {
            "The user has no team in this league."
        } else if (self.rank == 1) {
            "The user's team leads with ${self.totalPoints} pts."
        } else {
            "The user's team is #${self.rank} with ${self.totalPoints} pts, " +
                "${self.gapToLeader} pts behind the leader."
        }

        return buildString {
            append("You are Googly, a witty cricket-ball mascot for the FanLens AI fantasy companion.\n")
            append("CURRENT STATE (this is the only truth you may use):\n")
            append("- Match: ${match.matchTitle}, ${match.oversStr}, ${match.scoreStr}\n")
            append("- Standings:\n$table\n")
            append("- $standingLine\n")
            append("- User's top picks: $myPicks\n")
            append(
                "Answer in 2-3 sentences with light cricket banter. Use ONLY the numbers above. " +
                    "Never invent a score, a player total, an event or a prediction. " +
                    "If the answer is not in the state above, say you do not have that information."
            )
        }
    }

    private suspend fun queryOpenRouter(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("model", "nvidia/nemotron-3-ultra-550b-a55b:free")
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", buildSystemPrompt())
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                }
                put("messages", messages)
                put("max_tokens", 160)
            }

            val apiKey = System.getenv("OPENROUTER_API_KEY").orEmpty()
            if (apiKey.isBlank()) {
                return@withContext null
            }

            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://fanlens.ai")
                .header("X-Title", "FanLens AI")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val str = resp.body?.string().orEmpty()
                    val root = JSONObject(str)
                    val choices = root.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val choice = choices.getJSONObject(0)
                        val msg = choice.optJSONObject("message")
                        return@withContext msg?.optString("content")?.trim()
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Answers built from the engine, used when the model is unreachable.
     *
     * The wording changes with the question, but every number is read from the
     * same standings the screens show. Nothing here is a stored string, so a
     * fallback can never contradict the leaderboard.
     */
    private fun getGroundedFallback(question: String): String {
        val engine = com.fanlens.ai.data.scoring.FantasyScoringEngine
        val standings = engine.computeStandings()
        val self = standings.firstOrNull { it.isSelf }
        val leader = standings.firstOrNull()
        val q = question.lowercase()

        if (self == null) {
            return "I don't have a team for you in this league yet, so there's nothing to explain."
        }

        fun pickLabel(name: String) = name

        return when {
            q.contains("gap") || q.contains("catch") || q.contains("deficit") || q.contains("behind") ->
                if (self.rank == 1) {
                    "You lead with ${self.totalPoints} pts. The next team is ${standings.getOrNull(1)?.gapToLeader ?: "—"} pts back."
                } else {
                    "You're ${self.gapToLeader} pts behind ${leader?.teamName} (${leader?.totalPoints} pts)."
                }

            q.contains("rank") || q.contains("position") || q.contains("leader") || q.contains("why") ->
                "You're #${self.rank} of ${standings.size} with ${self.totalPoints} pts" +
                    if (self.rank == 1) ", leading the circle." else ", ${self.gapToLeader} pts behind ${leader?.teamName}."

            q.contains("score") || q.contains("top") || q.contains("mvp") || q.contains("pick") ||
                q.contains("player") ->
                self.fantasyTeamId.let { teamId ->
                    val picks = engine.getRoster(teamId).sortedByDescending { it.fantasyPoints }.take(3)
                    if (picks.isEmpty()) {
                        "No picks have scored for your team yet. You're #${self.rank} with ${self.totalPoints} pts."
                    } else {
                        "Your top scorers: " +
                            picks.joinToString(", ") { "${pickLabel(it.player.name)} ${it.fantasyPoints} pts" } +
                            ". Team total ${self.totalPoints} pts (#${self.rank})."
                    }
                }

            q.contains("what if") || q.contains("scenario") || q.contains("win") || q.contains("predict") ->
                "I can't predict a ball that hasn't happened. Right now you're #${self.rank} " +
                    "with ${self.totalPoints} pts, ${self.gapToLeader} pts behind ${leader?.teamName}."

            else -> {
                val m = engine.getMatchState()
                "It's ${m.oversStr}, ${m.scoreStr}. You're #${self.rank} of ${standings.size} " +
                    "with ${self.totalPoints} pts, ${self.gapToLeader} pts behind ${leader?.teamName}."
            }
        }
    }

    suspend fun explain(leagueId: String, question: String?): Result<InsightDto> = withContext(Dispatchers.IO) {
        val q = question.orEmpty()
        val aiResponse = if (q.isNotBlank()) queryOpenRouter(q) else null
        val text = aiResponse ?: getGroundedFallback(q)
        val source = if (aiResponse != null) "LLM" else "TEMPLATE"
        Result.success(
            InsightDto(
                id = "insight-${System.currentTimeMillis()}",
                triggerType = "MANUAL",
                text = text,
                source = source,
                eventIds = listOf("evt-106"),
                createdAt = "Just now",
            )
        )
    }

    /**
     * The latest proactive insight the device can justify: none.
     *
     * This used to return a fixed string labelled "LLM". Nothing generated it,
     * so it is reported honestly as a template and derived from live standings.
     */
    suspend fun recent(leagueId: String): Result<List<InsightDto>> = runCatching {
        val standings = com.fanlens.ai.data.scoring.FantasyScoringEngine.computeStandings()
        val self = standings.firstOrNull { it.isSelf } ?: return@runCatching emptyList()
        val leader = standings.firstOrNull() ?: return@runCatching emptyList()
        listOf(
            InsightDto(
                id = "insight-current",
                triggerType = "STANDINGS",
                text = if (self.rank == 1) {
                    "You lead with ${self.totalPoints} pts."
                } else {
                    "You're #${self.rank} with ${self.totalPoints} pts, " +
                        "${self.gapToLeader} pts behind ${leader.teamName}."
                },
                source = "TEMPLATE",
                eventIds = emptyList(),
                createdAt = "Now",
            )
        )
    }
}

class PreferencesRepository(
    private val api: FanLensApi,
    private val settingsStore: SettingsStore,
) {
    suspend fun setCompanionMode(companionMode: CompanionMode) {
        settingsStore.setCompanionMode(companionMode)
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        settingsStore.setOverlayEnabled(enabled)
    }

    suspend fun setRefreshPolicy(refreshPolicy: RefreshPolicy) {
        settingsStore.setRefreshPolicy(refreshPolicy)
    }

    suspend fun setTriggerCategories(categories: Set<TriggerCategory>) {
        settingsStore.setTriggerCategories(categories)
    }

    suspend fun setGapThreshold(value: Int) {
        settingsStore.setGapThreshold(value)
    }

    suspend fun setQuietHours(quietHours: QuietHours) {
        settingsStore.setQuietHours(quietHours)
    }

    suspend fun setMotionLevel(motionLevel: MotionLevel) {
        settingsStore.setMotionLevel(motionLevel)
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        settingsStore.setAiEnabled(enabled)
    }

    suspend fun setTone(tone: String) {
        settingsStore.setTone(tone)
    }

    suspend fun setWidgetFields(fields: Set<String>) {
        settingsStore.setWidgetFields(fields)
    }

    suspend fun setNotificationCategories(categories: Set<String>) {
        settingsStore.setNotificationCategories(categories)
    }
}
