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
public final class TorrentEngine_Factory implements Factory<TorrentEngine> {
  @Override
  public TorrentEngine get() {
    return newInstance();
  }

  public static TorrentEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TorrentEngine newInstance() {
    return new TorrentEngine();
  }

  private static final class InstanceHolder {
    private static final TorrentEngine_Factory INSTANCE = new TorrentEngine_Factory();
  }
}
