package com.fanlens.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fanlens.ai.data.prefs.CompanionMode
import com.fanlens.ai.data.prefs.RefreshPolicy
import com.fanlens.ai.data.prefs.SettingsStore
import com.fanlens.ai.data.prefs.TriggerCategory
import com.fanlens.ai.data.prefs.UserSettings
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

    init {
        viewModelScope.launch {
            settings.collect { current ->
                _uiState.value = _uiState.value.copy(
                    leagueId = current.activeLeagueId.ifBlank { _uiState.value.leagueId },
                    matchId = current.activeMatchId.ifBlank { _uiState.value.matchId },
                )
            }
        }
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
            .onSuccess { id ->
                _uiState.value = _uiState.value.copy(loading = false, leagueId = id, message = "league created")
            }
            .onFailure { ex ->
                _uiState.value = _uiState.value.copy(loading = false, error = ex.message ?: "create failed")
            }
    }

    fun joinLeague(leagueId: String, inviteCode: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true, error = "")
        leagueRepository.joinLeague(leagueId, inviteCode)
            .onSuccess {
                _uiState.value = _uiState.value.copy(loading = false, leagueId = leagueId, message = "joined")
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

    fun ask(question: String) = viewModelScope.launch {
        val state = _uiState.value
        if (state.leagueId.isBlank()) return@launch
        if (!settings.value.aiEnabled) {
            _insight.value = "AI insights are off. Turn on AI explanations in Settings to ask."
            return@launch
        }
        insightRepository.explain(state.leagueId, question)
            .onSuccess { insight -> _insight.value = insight.text }
            .onFailure { ex ->
                _insight.value = "Template: " + (ex.message ?: "explanation unavailable")
            }
    }

    fun setMatchId(matchId: String) {
        _uiState.value = _uiState.value.copy(matchId = matchId)
        viewModelScope.launch { settingsStore.setActiveLeague(_uiState.value.leagueId, matchId) }
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
