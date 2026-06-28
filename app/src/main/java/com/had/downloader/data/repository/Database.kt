package com.had.downloader.data.repository

import androidx.room.*
import com.had.downloader.data.model.DownloadItem
import com.had.downloader.data.model.DownloadMode
import com.had.downloader.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    suspend fun getAllSync(): List<DownloadItem>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: DownloadStatus): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DownloadItem): Long

    @Update
    suspend fun update(item: DownloadItem)

    @Delete
    suspend fun delete(item: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        "UPDATE downloads SET status = :status, progress = :progress, " +
                "downloadedBytes = :downloaded, totalBytes = :total, " +
                "speedBps = :speed, etaSeconds = :eta WHERE id = :id"
    )
    suspend fun updateProgress(
        id: Long, status: DownloadStatus, progress: Float,
        downloaded: Long, total: Long, speed: Long, eta: Int
    )

    @Query("UPDATE downloads SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: Long, status: DownloadStatus, completedAt: Long)

    @Query("UPDATE downloads SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun markFailed(id: Long, status: DownloadStatus, error: String?)

    @Query("SELECT COUNT(*) FROM downloads")
    fun countAll(): Flow<Int>

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'COMPLETED'")
    fun countCompleted(): Flow<Int>

    @Query("SELECT SUM(downloadedBytes) FROM downloads WHERE status = 'COMPLETED'")
    fun totalDownloaded(): Flow<Long?>
}

@Database(
    entities     = [DownloadItem::class, AnalyticsEvent::class, SpeedSample::class],
    version      = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HADDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun analyticsDao(): AnalyticsDao
}

class Converters {
    @TypeConverter fun fromStatus(s: DownloadStatus): String = s.name
    @TypeConverter fun toStatus(s: String): DownloadStatus =
        runCatching { DownloadStatus.valueOf(s) }.getOrDefault(DownloadStatus.FAILED)

    @TypeConverter fun fromMode(m: DownloadMode): String = m.name
    @TypeConverter fun toMode(s: String): DownloadMode =
        runCatching { DownloadMode.valueOf(s) }.getOrDefault(DownloadMode.HTTP)
}