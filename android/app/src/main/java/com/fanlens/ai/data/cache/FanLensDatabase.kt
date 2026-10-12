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
