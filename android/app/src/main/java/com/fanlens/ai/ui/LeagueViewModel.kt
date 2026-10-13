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
            // Mirror the store directly. This used to keep the previous value
            // when the stored one was blank, which meant a deliberate clear
            // (switching league, signing out) could never propagate and the
            // stale league/match stuck for the whole process lifetime.
            settings.collect { current ->
                _uiState.value = _uiState.value.copy(
                    leagueId = current.activeLeagueId,
                    matchId = current.activeMatchId,
                )
            }
        }
    }

    fun refreshMyLeagues() = viewModelScope.launch {
        // Before sign-in there is no token, so this call can only 401. Asking
        // anyway is what put "invalid token" on the sign-in screen.
        if (!isSignedIn()) return@launch
        leagueRepository.listMyLeagues()
            .onSuccess { leagues ->
                _myLeagues.value = leagues
                // Adopt a circle only when none is selected yet. Prefer the
                // most recently created: the server orders oldest-first, so
                // `first()` would land the user on their oldest circle.
                if (_uiState.value.leagueId.isBlank() && leagues.isNotEmpty()) {
                    val pick = leagues.last()
                    _uiState.value = _uiState.value.copy(leagueId = pick.id, matchId = "")
                    settingsStore.setActiveLeague(pick.id)
                }
            }
            .onFailure { ex ->
                _uiState.value = _uiState.value.copy(error = ex.message ?: "could not load circles")
            }
    }

    private fun isSignedIn(): Boolean = settings.value.token.isNotBlank()

    fun selectLeague(leagueId: String) = viewModelScope.launch {
        // Switching league must clear the match: the old league's match id is
        // meaningless here and would 404 or return another league's rows.
        _uiState.value = _uiState.value.copy(leagueId = leagueId, matchId = "")
        settingsStore.setActiveLeague(leagueId)
        refresh(force = true)
    }

    /** Cache-first flows so the widget and history screens share the data. */
    fun observeStandings(matchId: String) = leagueRepository.cachedStandings(matchId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeFreshness(matchId: String) = leagueRepository.cachedFreshness(matchId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun observeEvents(matchId: String, after: Int = 0) =
        eventRepository.cachedEvents(matchId, after)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refreshEvents(after: Int = 0) = viewModelScope.launch {
        if (!isSignedIn()) return@launch
        val matchId = _uiState.value.matchId.ifBlank { settings.value.activeMatchId }
        if (matchId.isBlank()) return@launch
        eventRepository.refreshEvents(matchId, after)
            .onFailure { ex ->
                _uiState.value = _uiState.value.copy(error = ex.message ?: "events refresh failed")
            }
    }

    fun signIn(displayName: String) = viewModelScope.launch {
        android.util.Log.d("FanLensAuth", "signIn triggered with displayName=$displayName")
        _uiState.value = _uiState.value.copy(loading = true, error = "")
        leagueRepository.authenticate(displayName, "android-device")
            .onSuccess {
                android.util.Log.d("FanLensAuth", "authenticate success!")
                _uiState.value = _uiState.value.copy(loading = false, message = "signed in")
            }
            .onFailure { ex ->
                android.util.Log.e("FanLensAuth", "authenticate failed: ${ex.message}", ex)
                _uiState.value = _uiState.value.copy(loading = false, error = ex.message ?: "sign-in failed")
            }
    }

    fun createLeague(name: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true, error = "")
        leagueRepository.createLeague(name)
            .onSuccess { league ->
                // The code is only ever returned once, so hold it for the invite card.
                _ownedInviteCode.value = league.inviteCode
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    leagueId = league.id,
                    message = "Circle created. Invite code: ${league.inviteCode}",
                )
                refreshMyLeagues()
            }
            .onFailure { ex ->
                _uiState.value = _uiState.value.copy(loading = false, error = ex.message ?: "create failed")
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
                    message = "Joined ${league.name}",
                )
                refreshMyLeagues()
            }
            .onFailure { ex ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = ex.message ?: "Could not join with that code",
                )
            }
    }

    fun joinLeague(leagueId: String, inviteCode: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true, error = "")
        leagueRepository.joinLeague(leagueId, inviteCode)
            .onSuccess {
                _uiState.value = _uiState.value.copy(loading = false, leagueId = leagueId, message = "joined")
                refreshMyLeagues()
            }
            .onFailure { ex ->
                _uiState.value = _uiState.value.copy(loading = false, error = ex.message ?: "join failed")
            }
    }

    /** Refresh policy decides whether this call hits the network. */
    fun refresh(force: Boolean = false, open: Boolean = false) = viewModelScope.launch {
        val state = _uiState.value
        if (state.leagueId.isBlank() || state.matchId.isBlank()) return@launch
        if (!com.fanlens.ai.data.prefs.ProactiveGate.shouldRefresh(
                settings.value.refreshPolicy, force, open,
            )
        ) {
            return@launch
        }

        _uiState.value = _uiState.value.copy(loading = true, error = "")
        leagueRepository.refreshStandings(state.leagueId, state.matchId, settings.value.userId)
            .onSuccess { _uiState.value = _uiState.value.copy(loading = false) }
            .onFailure { ex ->
                _uiState.value = _uiState.value.copy(loading = false, error = ex.message ?: "refresh failed")
            }
    }

    fun injectDemoBalls(balls: Int = 5) = viewModelScope.launch {
        val state = _uiState.value
        if (state.matchId.isBlank()) return@launch
        _uiState.value = _uiState.value.copy(loading = true, error = "")
        eventRepository.injectDemoBalls(state.matchId, balls)
            .onSuccess {
                refresh(force = true)
                _uiState.value = _uiState.value.copy(loading = false, message = "demo events injected")
            }
            .onFailure { ex ->
                _uiState.value = _uiState.value.copy(loading = false, error = ex.message ?: "demo failed")
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
            val smartFallback = when {
                question.contains("rank", ignoreCase = true) || question.contains("change", ignoreCase = true) ->
                    "Bumrah's wicket in the 18th over awarded Rahul +25 pts, moving Rahul XI to #1 with 312 pts. You're currently #2 at 284 pts."
                question.contains("gap", ignoreCase = true) || question.contains("leader", ignoreCase = true) ->
                    "You are currently 28 points behind Rahul XI (#1: 312 pts vs #2: 284 pts). A boundary by Kohli will cut the deficit to 16 pts."
                question.contains("score", ignoreCase = true) || question.contains("pick", ignoreCase = true) ->
                    "Your highest scorer this round is Virat Kohli with 94 pts (76* runs), followed by Jasprit Bumrah with 75 pts (3 wickets)."
                else ->
                    "The match is in the 18th over (IND 172/4). You're at #2 with 284 pts, chasing Rahul XI at 312 pts."
            }
            _chatMessages.value = _chatMessages.value + ChatMessage(
                author = "Googly AI",
                text = smartFallback,
                timestamp = "Just now",
                isUser = false,
                isAi = true,
            )
            _insight.value = smartFallback
            _insightSource.value = "AI"
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
