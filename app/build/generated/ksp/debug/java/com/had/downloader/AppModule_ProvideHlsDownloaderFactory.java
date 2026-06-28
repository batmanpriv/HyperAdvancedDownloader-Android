package com.had.downloader;

import com.had.downloader.service.HlsDownloader;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideHlsDownloaderFactory implements Factory<HlsDownloader> {
  @Override
  public HlsDownloader get() {
    return provideHlsDownloader();
  }

  public static AppModule_ProvideHlsDownloaderFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static HlsDownloader provideHlsDownloader() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideHlsDownloader());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideHlsDownloaderFactory INSTANCE = new AppModule_ProvideHlsDownloaderFactory();
  }
}
