package com.fanlens.ai.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceAuthRequest(
    @SerialName("display_name") val displayName: String,
    @SerialName("device_id") val deviceId: String,
)

@Serializable
data class DeviceAuthResponse(
    @SerialName("user_id") val userId: String,
    val token: String,
)

@Serializable
data class LeagueCreateRequest(val name: String)

@Serializable
data class LeagueCreatedResponse(
    val id: String,
    val name: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("invite_expires_at") val inviteExpiresAt: String,
)

@Serializable
data class MemberResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val role: String,
)

@Serializable
data class LeagueDetailResponse(
    val id: String,
    val name: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("created_at") val createdAt: String,
    val members: List<MemberResponse>,
)

@Serializable
data class JoinRequest(@SerialName("invite_code") val inviteCode: String)

@Serializable
data class RosterPlayerRequest(
    @SerialName("player_id") val playerId: String,
    val role: String,
    @SerialName("is_captain") val isCaptain: Boolean = false,
    @SerialName("is_vice") val isVice: Boolean = false,
)

@Serializable
data class RosterRequest(
    @SerialName("match_id") val matchId: String,
    @SerialName("team_name") val teamName: String,
    @SerialName("ruleset_id") val rulesetId: String,
    val players: List<RosterPlayerRequest>,
)

@Serializable
data class RosterPlayerResponse(
    @SerialName("player_id") val playerId: String,
    val role: String,
    @SerialName("is_captain") val isCaptain: Boolean,
    @SerialName("is_vice") val isVice: Boolean,
)

@Serializable
data class RosterResponse(
    val id: String,
    @SerialName("match_id") val matchId: String,
    @SerialName("team_name") val teamName: String,
    @SerialName("ruleset_id") val rulesetId: String,
    val players: List<RosterPlayerResponse>,
)

@Serializable
data class StandingRowDto(
    val rank: Int,
    @SerialName("fantasy_team_id") val fantasyTeamId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("team_name") val teamName: String,
    @SerialName("total_points") val totalPoints: String,
    @SerialName("gap_to_leader") val gapToLeader: String,
    @SerialName("delta_vs_previous") val deltaVsPrevious: String,
)

@Serializable
data class StandingsResponse(
    @SerialName("match_id") val matchId: String,
    val freshness: String,
    @SerialName("freshness_updated_at") val freshnessUpdatedAt: String? = null,
    val rows: List<StandingRowDto>,
)

@Serializable
data class EventDto(
    @SerialName("provider_event_id") val providerEventId: String,
    val sequence: Int,
    @SerialName("event_type") val eventType: String,
    @SerialName("occurred_at") val occurredAt: String,
    val payload: Map<String, String> = emptyMap(),
    val status: String,
)

@Serializable
data class EventListResponse(
    @SerialName("match_id") val matchId: String,
    val events: List<EventDto>,
)

@Serializable
data class EventIngestRequest(
    @SerialName("provider_event_id") val providerEventId: String,
    val sequence: Int,
    @SerialName("occurred_at") val occurredAt: String,
    val batter: String,
    val bowler: String,
    val fielder: String? = null,
    val runs: Int = 0,
    val wicket: Boolean = false,
    @SerialName("wicket_kind") val wicketKind: String? = null,
    @SerialName("event_type") val eventType: String = "BALL",
)

@Serializable
data class EventIngestResponse(
    val status: String,
    @SerialName("event_id") val eventId: String? = null,
    @SerialName("ledger_rows") val ledgerRows: Int = 0,
)

@Serializable
data class InsightDto(
    val id: String,
    @SerialName("trigger_type") val triggerType: String,
    val text: String,
    val source: String,
    @SerialName("event_ids") val eventIds: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class InsightListResponse(
    @SerialName("league_id") val leagueId: String,
    val insights: List<InsightDto>,
)

@Serializable
data class ExplainRequest(
    @SerialName("match_id") val matchId: String? = null,
    val question: String? = null,
)

@Serializable
data class PreferencesRequest(
    @SerialName("companion_mode") val companionMode: String? = null,
    @SerialName("overlay_enabled") val overlayEnabled: Boolean? = null,
    @SerialName("refresh_policy") val refreshPolicy: String? = null,
    @SerialName("trigger_categories") val triggerCategories: List<String>? = null,
    @SerialName("gap_threshold") val gapThreshold: Int? = null,
    @SerialName("quiet_hours") val quietHours: Map<String, String>? = null,
    @SerialName("motion_level") val motionLevel: String? = null,
    @SerialName("widget_fields") val widgetFields: List<String>? = null,
    @SerialName("notification_categories") val notificationCategories: List<String>? = null,
    @SerialName("ai_enabled") val aiEnabled: Boolean? = null,
    val tone: String? = null,
)

@Serializable
data class PreferencesResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("companion_mode") val companionMode: String,
    @SerialName("overlay_enabled") val overlayEnabled: Boolean,
    @SerialName("refresh_policy") val refreshPolicy: String,
    @SerialName("trigger_categories") val triggerCategories: List<String>,
    @SerialName("gap_threshold") val gapThreshold: Int,
    @SerialName("quiet_hours") val quietHours: Map<String, String>,
    @SerialName("motion_level") val motionLevel: String,
    @SerialName("widget_fields") val widgetFields: List<String>,
    @SerialName("notification_categories") val notificationCategories: List<String>,
    @SerialName("ai_enabled") val aiEnabled: Boolean,
    val tone: String,
)
