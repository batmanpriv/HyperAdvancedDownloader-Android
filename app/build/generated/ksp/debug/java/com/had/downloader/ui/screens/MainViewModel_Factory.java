package com.had.downloader.ui.screens;

import android.app.Application;
import com.had.downloader.data.repository.AnalyticsRepository;
import com.had.downloader.data.repository.DownloadDao;
import com.had.downloader.service.ClipboardMonitor;
import com.had.downloader.service.DuplicateDetector;
import com.had.downloader.service.FileSizeFetcher;
import com.had.downloader.service.HlsDownloader;
import com.had.downloader.service.RemoteDownloadServer;
import com.had.downloader.service.ScraperEngine;
import com.had.downloader.service.SmartDownloader;
import com.had.downloader.service.WebArchiveEngine;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class MainViewModel_Factory implements Factory<MainViewModel> {
  private final Provider<Application> appProvider;

  private final Provider<DownloadDao> daoProvider;

  private final Provider<SmartDownloader> smartDownloaderProvider;

  private final Provider<HlsDownloader> hlsDownloaderProvider;

  private final Provider<ScraperEngine> scraperEngineProvider;

  private final Provider<AnalyticsRepository> analyticsRepositoryProvider;

  private final Provider<RemoteDownloadServer> remoteDownloadServerProvider;

  private final Provider<ClipboardMonitor> clipboardMonitorProvider;

  private final Provider<DuplicateDetector> duplicateDetectorProvider;

  private final Provider<WebArchiveEngine> webArchiveEngineProvider;

  private final Provider<FileSizeFetcher> fileSizeFetcherProvider;

  public MainViewModel_Factory(Provider<Application> appProvider, Provider<DownloadDao> daoProvider,
      Provider<SmartDownloader> smartDownloaderProvider,
      Provider<HlsDownloader> hlsDownloaderProvider, Provider<ScraperEngine> scraperEngineProvider,
      Provider<AnalyticsRepository> analyticsRepositoryProvider,
      Provider<RemoteDownloadServer> remoteDownloadServerProvider,
      Provider<ClipboardMonitor> clipboardMonitorProvider,
      Provider<DuplicateDetector> duplicateDetectorProvider,
      Provider<WebArchiveEngine> webArchiveEngineProvider,
      Provider<FileSizeFetcher> fileSizeFetcherProvider) {
    this.appProvider = appProvider;
    this.daoProvider = daoProvider;
    this.smartDownloaderProvider = smartDownloaderProvider;
    this.hlsDownloaderProvider = hlsDownloaderProvider;
    this.scraperEngineProvider = scraperEngineProvider;
    this.analyticsRepositoryProvider = analyticsRepositoryProvider;
    this.remoteDownloadServerProvider = remoteDownloadServerProvider;
    this.clipboardMonitorProvider = clipboardMonitorProvider;
    this.duplicateDetectorProvider = duplicateDetectorProvider;
    this.webArchiveEngineProvider = webArchiveEngineProvider;
    this.fileSizeFetcherProvider = fileSizeFetcherProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(appProvider.get(), daoProvider.get(), smartDownloaderProvider.get(), hlsDownloaderProvider.get(), scraperEngineProvider.get(), analyticsRepositoryProvider.get(), remoteDownloadServerProvider.get(), clipboardMonitorProvider.get(), duplicateDetectorProvider.get(), webArchiveEngineProvider.get(), fileSizeFetcherProvider.get());
  }

  public static MainViewModel_Factory create(Provider<Application> appProvider,
      Provider<DownloadDao> daoProvider, Provider<SmartDownloader> smartDownloaderProvider,
      Provider<HlsDownloader> hlsDownloaderProvider, Provider<ScraperEngine> scraperEngineProvider,
      Provider<AnalyticsRepository> analyticsRepositoryProvider,
      Provider<RemoteDownloadServer> remoteDownloadServerProvider,
      Provider<ClipboardMonitor> clipboardMonitorProvider,
      Provider<DuplicateDetector> duplicateDetectorProvider,
      Provider<WebArchiveEngine> webArchiveEngineProvider,
      Provider<FileSizeFetcher> fileSizeFetcherProvider) {
    return new MainViewModel_Factory(appProvider, daoProvider, smartDownloaderProvider, hlsDownloaderProvider, scraperEngineProvider, analyticsRepositoryProvider, remoteDownloadServerProvider, clipboardMonitorProvider, duplicateDetectorProvider, webArchiveEngineProvider, fileSizeFetcherProvider);
  }

  public static MainViewModel newInstance(Application app, DownloadDao dao,
      SmartDownloader smartDownloader, HlsDownloader hlsDownloader, ScraperEngine scraperEngine,
      AnalyticsRepository analyticsRepository, RemoteDownloadServer remoteDownloadServer,
      ClipboardMonitor clipboardMonitor, DuplicateDetector duplicateDetector,
      WebArchiveEngine webArchiveEngine, FileSizeFetcher fileSizeFetcher) {
    return new MainViewModel(app, dao, smartDownloader, hlsDownloader, scraperEngine, analyticsRepository, remoteDownloadServer, clipboardMonitor, duplicateDetector, webArchiveEngine, fileSizeFetcher);
  }
}
