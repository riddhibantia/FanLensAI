package com.fanlens.ai.data.network

import com.fanlens.ai.di.TokenStore
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ApiException(val statusCode: Int, message: String) : IOException(message)

class FanLensApi(
    baseUrl: String,
    private val tokenProvider: () -> TokenStore,
) {
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
            val token = tokenProvider().token
            if (token.isNotBlank()) {
                builder.header("Authorization", "Bearer $token")
            }
            val response = chain.proceed(builder.build())
            if (response.code == 401) {
                tokenProvider().onUnauthorized?.invoke()
            }
            response
        }
        .build()

    private val root = baseUrl.toHttpUrl()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val jsonMediaType = "application/json".toMediaType()

    private suspend fun getJson(path: String, params: Map<String, String?> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            val urlBuilder = root.newBuilder().addPathSegments(path.trimStart('/'))
            params.forEach { (key, value) -> if (value != null) urlBuilder.addQueryParameter(key, value) }
            val request = Request.Builder().url(urlBuilder.build()).get().build()
            execute(request)
        }

    private suspend fun <T> sendJson(
        method: String,
        path: String,
        serializer: SerializationStrategy<T>,
        body: T,
    ): String =
        withContext(Dispatchers.IO) {
            val payload = json.encodeToString(serializer, body).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(root.newBuilder().addPathSegments(path.trimStart('/')).build())
                .method(method, if (method in setOf("GET", "HEAD")) null else payload)
                .build()
            execute(request)
        }

    private suspend fun postEmpty(path: String, params: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            val urlBuilder = root.newBuilder().addPathSegments(path.trimStart('/'))
            params.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }
            val request = Request.Builder()
                .url(urlBuilder.build())
                .post("".toRequestBody(jsonMediaType))
                .build()
            execute(request)
        }

    private fun execute(request: Request): String {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException(response.code, body)
            return body
        }
    }

    private fun <T> decode(raw: String, deserializer: kotlinx.serialization.DeserializationStrategy<T>): T =
        json.decodeFromString(deserializer, raw.ifBlank { "{}" })

    // ---- auth ----
    suspend fun authDevice(displayName: String, deviceId: String): DeviceAuthResponse {
        val body = sendJson(
            "POST", "v1/auth/device",
            DeviceAuthRequest.serializer(), DeviceAuthRequest(displayName, deviceId),
        )
        return decode(body, DeviceAuthResponse.serializer())
    }

    // ---- leagues ----
    suspend fun createLeague(name: String): LeagueCreatedResponse {
        val body = sendJson(
            "POST", "v1/leagues",
            LeagueCreateRequest.serializer(), LeagueCreateRequest(name),
        )
        return decode(body, LeagueCreatedResponse.serializer())
    }

    suspend fun joinLeague(leagueId: String, inviteCode: String) {
        sendJson(
            "POST", "v1/leagues/$leagueId/join",
            JoinRequest.serializer(), JoinRequest(inviteCode),
        )
    }

    suspend fun getLeague(leagueId: String): LeagueDetailResponse {
        val body = getJson("v1/leagues/$leagueId")
        return decode(body, LeagueDetailResponse.serializer())
    }

    // ---- rosters ----
    suspend fun upsertRoster(leagueId: String, request: RosterRequest): RosterResponse {
        val body = sendJson(
            "POST", "v1/leagues/$leagueId/rosters",
            RosterRequest.serializer(), request,
        )
        return decode(body, RosterResponse.serializer())
    }

    // ---- standings / events ----
    suspend fun getStandings(leagueId: String, matchId: String?): StandingsResponse {
        val body = getJson("v1/leagues/$leagueId/standings", mapOf("match_id" to matchId))
        return decode(body, StandingsResponse.serializer())
    }

    suspend fun getEvents(matchId: String, after: Int?): EventListResponse {
        val body = getJson(
            "v1/matches/$matchId/events",
            mapOf("after" to after?.toString()),
        )
        return decode(body, EventListResponse.serializer())
    }

    suspend fun ingestEvent(matchId: String, request: EventIngestRequest): EventIngestResponse {
        val body = sendJson(
            "POST", "v1/matches/$matchId/events",
            EventIngestRequest.serializer(), request,
        )
        return decode(body, EventIngestResponse.serializer())
    }

    suspend fun injectDemoEvents(matchId: String, balls: Int): List<EventIngestResponse> {
        val body = postEmpty(
            "v1/demo/matches/$matchId/events",
            mapOf("balls" to balls.toString()),
        )
        return json.decodeFromString(ListSerializer(EventIngestResponse.serializer()), body)
    }

    // ---- insights ----
    suspend fun listInsights(leagueId: String): InsightListResponse {
        val body = getJson("v1/leagues/$leagueId/insights")
        return decode(body, InsightListResponse.serializer())
    }

    suspend fun explain(leagueId: String, request: ExplainRequest): InsightDto {
        val body = sendJson(
            "POST", "v1/leagues/$leagueId/insights/explain",
            ExplainRequest.serializer(), request,
        )
        return decode(body, InsightDto.serializer())
    }

    // ---- preferences ----
    suspend fun patchPreferences(request: PreferencesRequest): PreferencesResponse {
        val body = sendJson(
            "PATCH", "v1/me/preferences",
            PreferencesRequest.serializer(), request,
        )
        return decode(body, PreferencesResponse.serializer())
    }

    // ---- account ----
    suspend fun deleteAccount(): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(root.newBuilder().addPathSegments("v1/me").build())
                .delete()
                .build()
            execute(request)
        }
}
