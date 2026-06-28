package com.had.downloader;

import com.had.downloader.service.SmartDownloader;
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
public final class AppModule_ProvideSmartDownloaderFactory implements Factory<SmartDownloader> {
  @Override
  public SmartDownloader get() {
    return provideSmartDownloader();
  }

  public static AppModule_ProvideSmartDownloaderFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SmartDownloader provideSmartDownloader() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSmartDownloader());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideSmartDownloaderFactory INSTANCE = new AppModule_ProvideSmartDownloaderFactory();
  }
}
