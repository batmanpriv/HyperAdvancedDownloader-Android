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
public final class SmartDownloader_Factory implements Factory<SmartDownloader> {
  @Override
  public SmartDownloader get() {
    return newInstance();
  }

  public static SmartDownloader_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SmartDownloader newInstance() {
    return new SmartDownloader();
  }

  private static final class InstanceHolder {
    private static final SmartDownloader_Factory INSTANCE = new SmartDownloader_Factory();
  }
}
