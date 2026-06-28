package com.had.downloader.service;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class DownloadSchedulerService_MembersInjector implements MembersInjector<DownloadSchedulerService> {
  private final Provider<SmartDownloader> smartDownloaderProvider;

  private final Provider<HlsDownloader> hlsDownloaderProvider;

  public DownloadSchedulerService_MembersInjector(Provider<SmartDownloader> smartDownloaderProvider,
      Provider<HlsDownloader> hlsDownloaderProvider) {
    this.smartDownloaderProvider = smartDownloaderProvider;
    this.hlsDownloaderProvider = hlsDownloaderProvider;
  }

  public static MembersInjector<DownloadSchedulerService> create(
      Provider<SmartDownloader> smartDownloaderProvider,
      Provider<HlsDownloader> hlsDownloaderProvider) {
    return new DownloadSchedulerService_MembersInjector(smartDownloaderProvider, hlsDownloaderProvider);
  }

  @Override
  public void injectMembers(DownloadSchedulerService instance) {
    injectSmartDownloader(instance, smartDownloaderProvider.get());
    injectHlsDownloader(instance, hlsDownloaderProvider.get());
  }

  @InjectedFieldSignature("com.had.downloader.service.DownloadSchedulerService.smartDownloader")
  public static void injectSmartDownloader(DownloadSchedulerService instance,
      SmartDownloader smartDownloader) {
    instance.smartDownloader = smartDownloader;
  }

  @InjectedFieldSignature("com.had.downloader.service.DownloadSchedulerService.hlsDownloader")
  public static void injectHlsDownloader(DownloadSchedulerService instance,
      HlsDownloader hlsDownloader) {
    instance.hlsDownloader = hlsDownloader;
  }
}
