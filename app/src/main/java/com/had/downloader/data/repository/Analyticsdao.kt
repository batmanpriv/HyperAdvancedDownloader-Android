package com.had.downloader.data.repository

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "analytics_events")
data class AnalyticsEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val downloadId: Long,
    val filename: String,
    val url: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val durationMs: Long = 0,
    val totalBytes: Long = 0,
    val avgSpeedBps: Long = 0,
    val peakSpeedBps: Long = 0,
    val threads: Int = 1,
    val retries: Int = 0,
    val success: Boolean = false,
    val mode: String = "HTTP",
    val dayOfWeek: Int = 0,
    val hourOfDay: Int = 0,
    val monthYear: String = ""
)

@Entity(tableName = "speed_samples")
data class SpeedSample(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val downloadId: Long,
    val timestamp: Long,
    val speedBps: Long
)

@Dao
interface AnalyticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AnalyticsEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeedSample(sample: SpeedSample)

    @Query("SELECT * FROM analytics_events ORDER BY startedAt DESC LIMIT 200")
    fun observeRecentEvents(): Flow<List<AnalyticsEvent>>

    @Query("SELECT * FROM analytics_events WHERE success = 1 ORDER BY startedAt DESC LIMIT 100")
    fun observeCompletedEvents(): Flow<List<AnalyticsEvent>>

    @Query("SELECT * FROM speed_samples WHERE downloadId = :id ORDER BY timestamp ASC")
    fun observeSpeedHistory(id: Long): Flow<List<SpeedSample>>

    @Query("SELECT * FROM speed_samples WHERE downloadId = :id ORDER BY timestamp ASC")
    suspend fun getSpeedHistory(id: Long): List<SpeedSample>

    @Query("SELECT monthYear, SUM(totalBytes) as bytes, COUNT(*) as count, AVG(avgSpeedBps) as avgSpeed FROM analytics_events WHERE success = 1 GROUP BY monthYear ORDER BY monthYear DESC LIMIT 12")
    fun observeMonthlyStats(): Flow<List<MonthlyStats>>

    @Query("SELECT hourOfDay, COUNT(*) as count FROM analytics_events WHERE success = 1 GROUP BY hourOfDay ORDER BY hourOfDay ASC")
    fun observeHourlyDistribution(): Flow<List<HourlyStats>>

    @Query("SELECT COUNT(*) as total, SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as successful, SUM(totalBytes) as totalBytes, AVG(avgSpeedBps) as avgSpeed, MAX(peakSpeedBps) as peakSpeed FROM analytics_events")
    fun observeOverallStats(): Flow<OverallStats>

    @Query("SELECT * FROM analytics_events WHERE downloadId = :id LIMIT 1")
    suspend fun getEventByDownloadId(id: Long): AnalyticsEvent?

    @Query("UPDATE analytics_events SET completedAt = :completedAt, durationMs = :durationMs, totalBytes = :totalBytes, avgSpeedBps = :avgSpeed, peakSpeedBps = :peakSpeed, success = :success, retries = :retries WHERE downloadId = :downloadId")
    suspend fun updateEvent(
        downloadId: Long,
        completedAt: Long,
        durationMs: Long,
        totalBytes: Long,
        avgSpeed: Long,
        peakSpeed: Long,
        success: Boolean,
        retries: Int
    )

    @Query("DELETE FROM speed_samples WHERE downloadId = :id")
    suspend fun deleteSpeedSamples(id: Long)

    @Query("DELETE FROM analytics_events WHERE startedAt < :before")
    suspend fun purgeOldEvents(before: Long)

    @Query("DELETE FROM speed_samples WHERE timestamp < :before")
    suspend fun purgeOldSamples(before: Long)
}

data class MonthlyStats(
    val monthYear: String,
    val bytes: Long,
    val count: Int,
    val avgSpeed: Long
)

data class HourlyStats(
    val hourOfDay: Int,
    val count: Int
)

data class OverallStats(
    val total: Int = 0,
    val successful: Int = 0,
    val totalBytes: Long = 0,
    val avgSpeed: Long = 0,
    val peakSpeed: Long = 0
)