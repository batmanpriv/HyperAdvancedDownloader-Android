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
public final class FileSizeFetcher_Factory implements Factory<FileSizeFetcher> {
  @Override
  public FileSizeFetcher get() {
    return newInstance();
  }

  public static FileSizeFetcher_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FileSizeFetcher newInstance() {
    return new FileSizeFetcher();
  }

  private static final class InstanceHolder {
    private static final FileSizeFetcher_Factory INSTANCE = new FileSizeFetcher_Factory();
  }
}
