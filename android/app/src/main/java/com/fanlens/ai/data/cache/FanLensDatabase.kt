package com.fanlens.ai.data.cache

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_standing")
data class CachedStanding(
    @PrimaryKey val id: String,
    val matchId: String,
    val leagueId: String,
    val rank: Int,
    val fantasyTeamId: String,
    val userId: String,
    val teamName: String,
    val totalPoints: String,
    val gapToLeader: String,
    val deltaVsPrevious: String,
    val isSelf: Boolean,
)

@Entity(tableName = "cached_event")
data class CachedEvent(
    @PrimaryKey val providerEventId: String,
    val matchId: String,
    val sequence: Int,
    val eventType: String,
    val occurredAt: String,
    val payload: String,
    val status: String,
)

@Entity(tableName = "cached_freshness")
data class CachedFreshness(
    @PrimaryKey val matchId: String,
    val freshness: String,
    val updatedAt: String,
)

@Dao
interface CacheDao {
    @Query("SELECT * FROM cached_standing WHERE matchId = :matchId ORDER BY rank")
    fun standings(matchId: String): Flow<List<CachedStanding>>

    @Query("SELECT * FROM cached_standing WHERE matchId = :matchId AND isSelf = 1 LIMIT 1")
    fun selfStanding(matchId: String): Flow<CachedStanding?>

    @Upsert
    suspend fun upsertStandings(rows: List<CachedStanding>)

    @Query("DELETE FROM cached_standing WHERE matchId = :matchId")
    suspend fun clearStandings(matchId: String)

    @Query("SELECT * FROM cached_event WHERE matchId = :matchId AND sequence > :after ORDER BY sequence LIMIT :limit")
    fun events(matchId: String, after: Int, limit: Int): Flow<List<CachedEvent>>

    @Upsert
    suspend fun upsertEvents(events: List<CachedEvent>)

    @Query("SELECT * FROM cached_freshness WHERE matchId = :matchId")
    fun freshness(matchId: String): Flow<CachedFreshness?>

    @Upsert
    suspend fun upsertFreshness(freshness: CachedFreshness)

    @Query("SELECT COUNT(*) FROM cached_event WHERE matchId = :matchId")
    suspend fun eventCount(matchId: String): Int
}

@Database(
    entities = [CachedStanding::class, CachedEvent::class, CachedFreshness::class],
    version = 1,
    exportSchema = false,
)
abstract class FanLensDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao

    companion object {
        fun build(context: android.content.Context): FanLensDatabase =
            androidx.room.Room.databaseBuilder(
                context,
                FanLensDatabase::class.java,
                "fanlens.db",
            ).fallbackToDestructiveMigration().build()
    }
}

object MockDataSeeder {
    suspend fun seedDefaults(dao: CacheDao) {
        val matchId = "match-ind-aus-2026"
        val leagueId = "league-demo-1"

        val standings = listOf(
            CachedStanding(
                id = "${matchId}:team-siddhi",
                matchId = matchId,
                leagueId = leagueId,
                rank = 1,
                fantasyTeamId = "team-siddhi",
                userId = "u-siddhi",
                teamName = "Siddhi XI",
                totalPoints = "312",
                gapToLeader = "0",
                deltaVsPrevious = "+25",
                isSelf = false,
            ),
            CachedStanding(
                id = "${matchId}:team-riddhi",
                matchId = matchId,
                leagueId = leagueId,
                rank = 2,
                fantasyTeamId = "team-riddhi",
                userId = "u-riddhi",
                teamName = "Riddhi XI",
                totalPoints = "284",
                gapToLeader = "28",
                deltaVsPrevious = "+12",
                isSelf = true,
            ),
            CachedStanding(
                id = "${matchId}:team-nikki",
                matchId = matchId,
                leagueId = leagueId,
                rank = 3,
                fantasyTeamId = "team-nikki",
                userId = "u-nikki",
                teamName = "Nikki XI",
                totalPoints = "240",
                gapToLeader = "72",
                deltaVsPrevious = "+4",
                isSelf = false,
            ),
            CachedStanding(
                id = "${matchId}:team-nakshatra",
                matchId = matchId,
                leagueId = leagueId,
                rank = 4,
                fantasyTeamId = "team-nakshatra",
                userId = "u-nakshatra",
                teamName = "Nakshatra XI",
                totalPoints = "210",
                gapToLeader = "102",
                deltaVsPrevious = "-8",
                isSelf = false,
            ),
        )
        dao.upsertStandings(standings)

        dao.upsertFreshness(
            CachedFreshness(
                matchId = matchId,
                freshness = "LIVE",
                updatedAt = "Over 18.2 • IND 172/4",
            )
        )

        val events = listOf(
            CachedEvent(
                providerEventId = "evt-101",
                matchId = matchId,
                sequence = 101,
                eventType = "DELIVERY",
                occurredAt = "18:40:10",
                payload = """{"over":"17.3","ball":3,"runs":2,"striker":"V Kohli","bowler":"M Starc"}""",
                status = "OFFICIAL",
            ),
            CachedEvent(
                providerEventId = "evt-102",
                matchId = matchId,
                sequence = 102,
                eventType = "BOUNDARY_SIX",
                occurredAt = "18:40:45",
                payload = """{"over":"17.4","ball":4,"runs":6,"striker":"V Kohli","bowler":"M Starc","impact":"+14 pts Riddhi XI"}""",
                status = "OFFICIAL",
            ),
            CachedEvent(
                providerEventId = "evt-103",
                matchId = matchId,
                sequence = 103,
                eventType = "DOT_BALL",
                occurredAt = "18:41:20",
                payload = """{"over":"17.5","ball":5,"runs":0,"striker":"V Kohli","bowler":"M Starc"}""",
                status = "OFFICIAL",
            ),
            CachedEvent(
                providerEventId = "evt-104",
                matchId = matchId,
                sequence = 104,
                eventType = "BOUNDARY_FOUR",
                occurredAt = "18:42:00",
                payload = """{"over":"17.6","ball":6,"runs":4,"striker":"V Kohli","bowler":"M Starc","impact":"+10 pts Riddhi XI"}""",
                status = "OFFICIAL",
            ),
            CachedEvent(
                providerEventId = "evt-105",
                matchId = matchId,
                sequence = 105,
                eventType = "DELIVERY",
                occurredAt = "18:43:10",
                payload = """{"over":"18.1","ball":1,"runs":1,"striker":"H Pandya","bowler":"P Cummins"}""",
                status = "OFFICIAL",
            ),
            CachedEvent(
                providerEventId = "evt-106",
                matchId = matchId,
                sequence = 106,
                eventType = "WICKET",
                occurredAt = "18:44:00",
                payload = """{"over":"18.2","ball":2,"wicket":"c Kohli b Bumrah (Maxwell)","impact":"+25 pts Siddhi XI (#1)"}""",
                status = "OFFICIAL",
            ),
        )
        dao.upsertEvents(events)
    }
}
