package com.fanlens.ai.di

import android.content.Context
import com.fanlens.ai.BuildConfig
import com.fanlens.ai.data.cache.FanLensDatabase
import com.fanlens.ai.data.network.FanLensApi
import com.fanlens.ai.data.prefs.SettingsStore
import com.fanlens.ai.data.repository.EventRepository
import com.fanlens.ai.data.repository.InsightRepository
import com.fanlens.ai.data.repository.LeagueRepository
import com.fanlens.ai.data.repository.PreferencesRepository
import com.fanlens.ai.data.repository.RosterRepository
import kotlinx.coroutines.launch

/** Manual DI: one graph, created in Application.onCreate. */
object ServiceLocator {
    lateinit var api: FanLensApi
    lateinit var settingsStore: SettingsStore
    lateinit var database: FanLensDatabase
    lateinit var leagueRepository: LeagueRepository
    lateinit var rosterRepository: RosterRepository
    lateinit var eventRepository: EventRepository
    lateinit var insightRepository: InsightRepository
    lateinit var preferencesRepository: PreferencesRepository

    fun init(context: Context) {
        if (::api.isInitialized) return
        val app = context.applicationContext
        api = FanLensApi(
            baseUrl = BuildConfig.DEFAULT_BASE_URL,
            tokenProvider = { TokenStore }
        )
        settingsStore = SettingsStore(app)
        database = FanLensDatabase.build(app)
        leagueRepository = LeagueRepository(api, settingsStore, database.cacheDao())
        rosterRepository = RosterRepository(api, settingsStore)
        eventRepository = EventRepository(api, settingsStore, database.cacheDao())
        insightRepository = InsightRepository(api, settingsStore)
        preferencesRepository = PreferencesRepository(api, settingsStore)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.fanlens.ai.data.cache.MockDataSeeder.seedDefaults(database.cacheDao())
        }
    }
}

/** Resolved by AuthInterceptor from DataStore on every request. */
object TokenStore {
    @Volatile
    var token: String = ""
    var onUnauthorized: (() -> Unit)? = null
}
