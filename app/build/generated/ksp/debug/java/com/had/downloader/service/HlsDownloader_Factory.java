package com.had.downloader.service;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class HlsDownloader_Factory implements Factory<HlsDownloader> {
  @Override
  public HlsDownloader get() {
    return newInstance();
  }

  public static HlsDownloader_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static HlsDownloader newInstance() {
    return new HlsDownloader();
  }

  private static final class InstanceHolder {
    private static final HlsDownloader_Factory INSTANCE = new HlsDownloader_Factory();
  }
}
