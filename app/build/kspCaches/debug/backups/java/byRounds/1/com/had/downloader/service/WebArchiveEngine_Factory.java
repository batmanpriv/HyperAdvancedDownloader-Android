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
public final class WebArchiveEngine_Factory implements Factory<WebArchiveEngine> {
  @Override
  public WebArchiveEngine get() {
    return newInstance();
  }

  public static WebArchiveEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static WebArchiveEngine newInstance() {
    return new WebArchiveEngine();
  }

  private static final class InstanceHolder {
    private static final WebArchiveEngine_Factory INSTANCE = new WebArchiveEngine_Factory();
  }
}
