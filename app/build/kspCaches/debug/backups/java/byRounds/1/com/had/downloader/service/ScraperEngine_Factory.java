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
public final class ScraperEngine_Factory implements Factory<ScraperEngine> {
  @Override
  public ScraperEngine get() {
    return newInstance();
  }

  public static ScraperEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ScraperEngine newInstance() {
    return new ScraperEngine();
  }

  private static final class InstanceHolder {
    private static final ScraperEngine_Factory INSTANCE = new ScraperEngine_Factory();
  }
}
