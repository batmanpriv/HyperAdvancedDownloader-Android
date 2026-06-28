package com.had.downloader.service;

import com.had.downloader.data.repository.DownloadDao;
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
public final class ForegroundDownloadService_MembersInjector implements MembersInjector<ForegroundDownloadService> {
  private final Provider<DownloadDao> daoProvider;

  private final Provider<SmartDownloader> smartDownloaderProvider;

  private final Provider<HlsDownloader> hlsDownloaderProvider;

  public ForegroundDownloadService_MembersInjector(Provider<DownloadDao> daoProvider,
      Provider<SmartDownloader> smartDownloaderProvider,
      Provider<HlsDownloader> hlsDownloaderProvider) {
    this.daoProvider = daoProvider;
    this.smartDownloaderProvider = smartDownloaderProvider;
    this.hlsDownloaderProvider = hlsDownloaderProvider;
  }

  public static MembersInjector<ForegroundDownloadService> create(Provider<DownloadDao> daoProvider,
      Provider<SmartDownloader> smartDownloaderProvider,
      Provider<HlsDownloader> hlsDownloaderProvider) {
    return new ForegroundDownloadService_MembersInjector(daoProvider, smartDownloaderProvider, hlsDownloaderProvider);
  }

  @Override
  public void injectMembers(ForegroundDownloadService instance) {
    injectDao(instance, daoProvider.get());
    injectSmartDownloader(instance, smartDownloaderProvider.get());
    injectHlsDownloader(instance, hlsDownloaderProvider.get());
  }

  @InjectedFieldSignature("com.had.downloader.service.ForegroundDownloadService.dao")
  public static void injectDao(ForegroundDownloadService instance, DownloadDao dao) {
    instance.dao = dao;
  }

  @InjectedFieldSignature("com.had.downloader.service.ForegroundDownloadService.smartDownloader")
  public static void injectSmartDownloader(ForegroundDownloadService instance,
      SmartDownloader smartDownloader) {
    instance.smartDownloader = smartDownloader;
  }

  @InjectedFieldSignature("com.had.downloader.service.ForegroundDownloadService.hlsDownloader")
  public static void injectHlsDownloader(ForegroundDownloadService instance,
      HlsDownloader hlsDownloader) {
    instance.hlsDownloader = hlsDownloader;
  }
}
