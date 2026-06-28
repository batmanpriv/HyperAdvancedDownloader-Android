package com.had.downloader;

import com.had.downloader.service.FileSizeFetcher;
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
public final class AppModule_ProvideFileSizeFetcherFactory implements Factory<FileSizeFetcher> {
  @Override
  public FileSizeFetcher get() {
    return provideFileSizeFetcher();
  }

  public static AppModule_ProvideFileSizeFetcherFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FileSizeFetcher provideFileSizeFetcher() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideFileSizeFetcher());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideFileSizeFetcherFactory INSTANCE = new AppModule_ProvideFileSizeFetcherFactory();
  }
}
