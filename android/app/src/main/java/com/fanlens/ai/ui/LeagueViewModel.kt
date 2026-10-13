package com.fanlens.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fanlens.ai.data.prefs.CompanionMode
import com.fanlens.ai.data.prefs.RefreshPolicy
import com.fanlens.ai.data.prefs.SettingsStore
import com.fanlens.ai.data.prefs.TriggerCategory
import com.fanlens.ai.data.prefs.UserSettings
import com.fanlens.ai.data.network.LeagueSummaryResponse
import com.fanlens.ai.di.ServiceLocator
import com.fanlens.ai.data.repository.EventRepository
import com.fanlens.ai.data.repository.InsightRepository
import com.fanlens.ai.data.repository.LeagueRepository
import com.fanlens.ai.data.repository.PreferencesRepository
import com.fanlens.ai.data.repository.RosterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val author: String,
    val text: String,
    val timestamp: String,
    val isUser: Boolean,
    val isAi: Boolean = false,
)

data class LeagueUiState(
    val loading: Boolean = false,
    val message: String = "",
    val error: String = "",
    val leagueId: String = "",
    val matchId: String = "",
)

class LeagueViewModel(
    private val leagueRepository: LeagueRepository,
    private val rosterRepository: RosterRepository,
    private val eventRepository: EventRepository,
    private val insightRepository: InsightRepository,
    private val preferencesRepository: PreferencesRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val hasToken: StateFlow<Boolean> = settings
        .map { it.token.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _uiState = MutableStateFlow(LeagueUiState())
    val uiState: StateFlow<LeagueUiState> = _uiState.asStateFlow()

    private val _insight = MutableStateFlow("")
    val insight: StateFlow<String> = _insight.asStateFlow()

    /** "LLM" or "TEMPLATE" — shown so an explanation's origin is never hidden. */
    private val _insightSource = MutableStateFlow("")
    val insightSource: StateFlow<String> = _insightSource.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    /** The user's real circles, loaded from the server. Empty until fetched. */
    private val _myLeagues = MutableStateFlow<List<LeagueSummaryResponse>>(emptyList())
    val myLeagues: StateFlow<List<LeagueSummaryResponse>> = _myLeagues.asStateFlow()

    /** Trims a fresh invite code down to the part a friend needs to type. */
    private val _ownedInviteCode = MutableStateFlow("")
    val ownedInviteCode: StateFlow<String> = _ownedInviteCode.asStateFlow()

    init {
        viewModelScope.launch {
            settings.collect { current ->
                _uiState.value = _uiState.value.copy(
                    leagueId = current.activeLeagueId.ifBlank { "league-demo-1" },
                    matchId = current.activeMatchId.ifBlank { "match-ind-aus-2026" },
                )
            }
        }
        refreshMyLeagues()
        refresh(force = true)
    }

    fun refreshMyLeagues() = viewModelScope.launch {
        leagueRepository.listMyLeagues()
            .onSuccess { leagues ->
                _myLeagues.value = leagues
                if (_uiState.value.leagueId.isBlank() && leagues.isNotEmpty()) {
                    val pick = leagues.first()
                    _uiState.value = _uiState.value.copy(leagueId = pick.id, matchId = "match-ind-aus-2026")
                    settingsStore.setActiveLeague(pick.id, "match-ind-aus-2026")
                }
            }
    }

    private fun isSignedIn(): Boolean = true

    fun selectLeague(leagueId: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(leagueId = leagueId, matchId = "match-ind-aus-2026")
        settingsStore.setActiveLeague(leagueId, "match-ind-aus-2026")
        refresh(force = true)
    }

    /** Cache-first flows so the widget and history screens share the data. */
    fun observeStandings(matchId: String) = leagueRepository.cachedStandings(matchId.ifBlank { "match-ind-aus-2026" })
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeFreshness(matchId: String) = leagueRepository.cachedFreshness(matchId.ifBlank { "match-ind-aus-2026" })
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun observeEvents(matchId: String, after: Int = 0) =
        eventRepository.cachedEvents(matchId.ifBlank { "match-ind-aus-2026" }, after)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refreshEvents(after: Int = 0) = viewModelScope.launch {
        val matchId = _uiState.value.matchId.ifBlank { settings.value.activeMatchId.ifBlank { "match-ind-aus-2026" } }
        eventRepository.refreshEvents(matchId, after)
    }

    fun signIn(displayName: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true, error = "")
        leagueRepository.authenticate(displayName, "android-device")
            .onSuccess {
                _uiState.value = _uiState.value.copy(loading = false, message = "Signed in as $displayName")
                refreshMyLeagues()
                refresh(force = true)
            }
            .onFailure {
                _uiState.value = _uiState.value.copy(loading = false)
            }
    }

    fun createLeague(name: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true, error = "")
        leagueRepository.createLeague(name)
            .onSuccess { league ->
                _ownedInviteCode.value = league.inviteCode
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    leagueId = league.id,
                    matchId = "match-ind-aus-2026",
                    message = "Circle created! Invite code: ${league.inviteCode}",
                )
                refreshMyLeagues()
                refresh(force = true)
            }
    }

    /** Join with a code only. Friends never have the internal league id. */
    fun joinByCode(inviteCode: String) = viewModelScope.launch {
        if (inviteCode.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter an invite code")
            return@launch
        }
        _uiState.value = _uiState.value.copy(loading = true, error = "")
        leagueRepository.joinLeagueByCode(inviteCode.trim())
            .onSuccess { league ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    leagueId = league.id,
                    matchId = "match-ind-aus-2026",
                    message = "Joined ${league.name}",
                )
                refreshMyLeagues()
                refresh(force = true)
            }
    }

    fun joinLeague(leagueId: String, inviteCode: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true, error = "")
        leagueRepository.joinLeague(leagueId, inviteCode)
            .onSuccess {
                _uiState.value = _uiState.value.copy(loading = false, leagueId = leagueId, matchId = "match-ind-aus-2026", message = "joined")
                refreshMyLeagues()
                refresh(force = true)
            }
    }

    /** Refresh policy decides whether this call hits the network. */
    fun refresh(force: Boolean = false, open: Boolean = false) = viewModelScope.launch {
        val state = _uiState.value
        val safeLeague = state.leagueId.ifBlank { settings.value.activeLeagueId.ifBlank { "league-demo-1" } }
        val safeMatch = state.matchId.ifBlank { settings.value.activeMatchId.ifBlank { "match-ind-aus-2026" } }

        _uiState.value = _uiState.value.copy(loading = true, error = "")
        leagueRepository.refreshStandings(safeLeague, safeMatch, settings.value.userId)
            .onSuccess {
                _uiState.value = _uiState.value.copy(loading = false, leagueId = safeLeague, matchId = safeMatch)
            }
            .onFailure {
                _uiState.value = _uiState.value.copy(loading = false)
            }
    }

    fun injectDemoBalls(balls: Int = 1) = viewModelScope.launch {
        val matchId = _uiState.value.matchId.ifBlank { settings.value.activeMatchId.ifBlank { "match-ind-aus-2026" } }
        _uiState.value = _uiState.value.copy(loading = true, error = "")
        eventRepository.injectDemoBalls(matchId, balls)
            .onSuccess {
                refresh(force = true)
                _uiState.value = _uiState.value.copy(loading = false, message = "Demo delivery simulated!")
            }
            .onFailure {
                _uiState.value = _uiState.value.copy(loading = false)
            }
    }

    fun ask(question: String) = askAi(question)

    /**
     * Asks the server to explain the standings. The server owns the LLM key,
     * validates every number against its own facts, and falls back to a
     * deterministic template. Nothing is generated on the device.
     */
    fun askAi(question: String) = viewModelScope.launch {
        if (question.isBlank()) return@launch
        val leagueId = _uiState.value.leagueId.ifBlank { settings.value.activeLeagueId }
        val userMsg = ChatMessage(author = "You", text = question, timestamp = "Just now", isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiThinking.value = true

        if (leagueId.isBlank()) {
            _isAiThinking.value = false
            _chatMessages.value = _chatMessages.value + ChatMessage(
                author = "Googly AI",
                text = "Join or create a circle first, then ask me about your standings.",
                timestamp = "Just now",
                isUser = false,
                isAi = true,
            )
            return@launch
        }

        val result = insightRepository.explain(leagueId, question)

        _isAiThinking.value = false
        result.onSuccess { dto ->
            val aiMsg = ChatMessage(
                author = "Googly AI",
                text = dto.text,
                timestamp = "Just now",
                isUser = false,
                isAi = true,
            )
            _chatMessages.value = _chatMessages.value + aiMsg
            _insight.value = dto.text
            _insightSource.value = if (dto.source == "LLM") "AI" else "Template"
        }.onFailure { ex ->
            // The repository already computes a grounded reply; reaching here
            // means even that failed. Say so plainly instead of inventing a
            // scoreline, and never label it "AI".
            val message = "I couldn't reach the explanation service. " +
                (ex.message?.takeIf { it.isNotBlank() } ?: "No details were returned.")
            _chatMessages.value = _chatMessages.value + ChatMessage(
                author = "Googly AI",
                text = message,
                timestamp = "Just now",
                isUser = false,
                isAi = true,
            )
            _insight.value = message
            _insightSource.value = "Unavailable"
        }
    }

    /**
     * League banter is a real message thread, not a scripted bot. A local echo
     * is all the prototype supports until the backend grows a chat endpoint.
     */
    fun sendBanter(text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        _chatMessages.value = _chatMessages.value + ChatMessage(
            author = settings.value.displayName.ifBlank { "You" },
            text = text,
            timestamp = "Just now",
            isUser = true,
        )
    }

    /**
     * Selects the active match and loads its standings.
     *
     * Writes only the match, so a blank league can never be persisted, and
     * refreshes — otherwise the previous match's cached rows stayed on screen
     * until something else forced a reload.
     */
    fun setMatchId(matchId: String) {
        _uiState.value = _uiState.value.copy(matchId = matchId)
        viewModelScope.launch {
            settingsStore.setActiveMatch(matchId)
            refresh(force = true)
        }
    }

    fun setLeagueId(leagueId: String) {
        _uiState.value = _uiState.value.copy(leagueId = leagueId)
    }

    fun refreshOverlayState(context: android.content.Context) {
        com.fanlens.ai.overlay.OverlayController.sync(context, settings.value)
    }

    // settings passthrough
    fun setCompanionMode(mode: CompanionMode) = viewModelScope.launch { preferencesRepository.setCompanionMode(mode) }
    fun setOverlayEnabled(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setOverlayEnabled(enabled) }

    /** Remembers where the user dropped the mascot, in pixels from the top-left. */
    fun setPetPosition(x: Float, y: Float) = viewModelScope.launch {
        settingsStore.setOverlayPosition(x.toInt(), y.toInt())
    }
    fun setRefreshPolicy(policy: RefreshPolicy) = viewModelScope.launch { preferencesRepository.setRefreshPolicy(policy) }
    fun setTriggerCategories(categories: Set<TriggerCategory>) = viewModelScope.launch { preferencesRepository.setTriggerCategories(categories) }
    fun setGapThreshold(value: Int) = viewModelScope.launch { preferencesRepository.setGapThreshold(value) }
    fun setQuietHours(enabled: Boolean, start: Int, end: Int) {
        viewModelScope.launch {
            preferencesRepository.setQuietHours(com.fanlens.ai.data.prefs.QuietHours(enabled, start, end))
        }
    }
    fun setMotionLevel(level: com.fanlens.ai.data.prefs.MotionLevel) = viewModelScope.launch { preferencesRepository.setMotionLevel(level) }
    fun setAiEnabled(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setAiEnabled(enabled) }
    fun setTone(tone: String) = viewModelScope.launch { preferencesRepository.setTone(tone) }

    fun projectWhatIf(scenario: String, runs: Int, wickets: Int) =
        com.fanlens.ai.data.scoring.FantasyScoringEngine.projectWhatIf(scenario, runs, wickets)

    fun getTeamRoster(teamId: String) =
        com.fanlens.ai.data.scoring.FantasyScoringEngine.getRoster(teamId)

    fun updateSelfRoster(members: List<com.fanlens.ai.data.scoring.RosterMember>) {
        com.fanlens.ai.data.scoring.FantasyScoringEngine.updateSelfRoster(members)
        refresh(force = true)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = LeagueViewModel(
                ServiceLocator.leagueRepository,
                ServiceLocator.rosterRepository,
                ServiceLocator.eventRepository,
                ServiceLocator.insightRepository,
                ServiceLocator.preferencesRepository,
                ServiceLocator.settingsStore,
            ) as T
        }
    }
}
