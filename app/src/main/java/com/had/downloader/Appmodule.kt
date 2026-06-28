package com.had.downloader

import android.content.Context
import androidx.room.Room
import com.had.downloader.data.repository.AnalyticsDao
import com.had.downloader.data.repository.AnalyticsRepository
import com.had.downloader.data.repository.DownloadDao
import com.had.downloader.data.repository.HADDatabase
import com.had.downloader.service.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): HADDatabase =
        Room.databaseBuilder(ctx, HADDatabase::class.java, "had_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideDao(db: HADDatabase): DownloadDao = db.downloadDao()

    @Provides @Singleton
    fun provideAnalyticsDao(db: HADDatabase): AnalyticsDao = db.analyticsDao()

    @Provides @Singleton
    fun provideAnalyticsRepository(dao: AnalyticsDao): AnalyticsRepository = AnalyticsRepository(dao)

    @Provides @Singleton
    fun provideSmartDownloader(): SmartDownloader = SmartDownloader()

    @Provides @Singleton
    fun provideHlsDownloader(): HlsDownloader = HlsDownloader()

    @Provides @Singleton
    fun provideScraperEngine(): ScraperEngine = ScraperEngine()

    @Provides @Singleton
    fun provideVideoDetectionEngine(): VideoDetectionEngine = VideoDetectionEngine()

    @Provides @Singleton
    fun provideSmartRetryEngine(): SmartRetryEngine = SmartRetryEngine()

    @Provides @Singleton
    fun provideRemoteDownloadServer(): RemoteDownloadServer = RemoteDownloadServer()

    @Provides @Singleton
    fun provideClipboardMonitor(): ClipboardMonitor = ClipboardMonitor()

    @Provides @Singleton
    fun provideDuplicateDetector(dao: DownloadDao): DuplicateDetector = DuplicateDetector(dao)

    @Provides @Singleton
    fun provideFileSizeFetcher(): FileSizeFetcher = FileSizeFetcher()
}