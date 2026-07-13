package com.had.downloader.data.repository

import com.had.downloader.data.model.DownloadItem
import com.had.downloader.data.model.DownloadMode
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    private val analyticsDao: AnalyticsDao
) {

    fun observeRecentEvents(): Flow<List<AnalyticsEvent>> = analyticsDao.observeRecentEvents()
    fun observeCompletedEvents(): Flow<List<AnalyticsEvent>> = analyticsDao.observeCompletedEvents()
    fun observeMonthlyStats(): Flow<List<MonthlyStats>> = analyticsDao.observeMonthlyStats()
    fun observeHourlyDistribution(): Flow<List<HourlyStats>> = analyticsDao.observeHourlyDistribution()
    fun observeOverallStats(): Flow<OverallStats> = analyticsDao.observeOverallStats()
    fun observeSpeedHistory(downloadId: Long): Flow<List<SpeedSample>> = analyticsDao.observeSpeedHistory(downloadId)

    suspend fun recordStart(item: DownloadItem) {
        val cal = Calendar.getInstance()
        val existing = analyticsDao.getEventByDownloadId(item.id)
        if (existing != null) return
        analyticsDao.insertEvent(
            AnalyticsEvent(
                downloadId  = item.id,
                filename    = item.filename,
                url         = item.url,
                startedAt   = System.currentTimeMillis(),
                threads     = item.threads,
                mode        = item.mode.name,
                dayOfWeek   = cal.get(Calendar.DAY_OF_WEEK),
                hourOfDay   = cal.get(Calendar.HOUR_OF_DAY),
                monthYear   = "${cal.get(Calendar.YEAR)}-${"%02d".format(cal.get(Calendar.MONTH) + 1)}"
            )
        )
    }

    suspend fun recordComplete(
        downloadId: Long,
        totalBytes: Long,
        avgSpeedBps: Long,
        peakSpeedBps: Long,
        retries: Int,
        success: Boolean
    ) {
        val now = System.currentTimeMillis()
        val event = analyticsDao.getEventByDownloadId(downloadId) ?: return
        analyticsDao.updateEvent(
            downloadId  = downloadId,
            completedAt = now,
            durationMs  = now - event.startedAt,
            totalBytes  = totalBytes,
            avgSpeed    = avgSpeedBps,
            peakSpeed   = peakSpeedBps,
            success     = success,
            retries     = retries
        )
        analyticsDao.deleteSpeedSamples(downloadId)
    }

    suspend fun recordSpeedSample(downloadId: Long, speedBps: Long) {
        if (speedBps <= 0L) return
        analyticsDao.insertSpeedSample(
            SpeedSample(
                downloadId = downloadId,
                timestamp  = System.currentTimeMillis(),
                speedBps   = speedBps
            )
        )
    }

    suspend fun purgeOlderThan(days: Int = 30) {
        val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        analyticsDao.purgeOldEvents(cutoff)
        analyticsDao.purgeOldSamples(cutoff)
    }
}