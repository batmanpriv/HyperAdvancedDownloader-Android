package com.had.downloader.service;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class BrowserInterceptor_Factory implements Factory<BrowserInterceptor> {
  private final Provider<ScraperEngine> scraperEngineProvider;

  public BrowserInterceptor_Factory(Provider<ScraperEngine> scraperEngineProvider) {
    this.scraperEngineProvider = scraperEngineProvider;
  }

  @Override
  public BrowserInterceptor get() {
    return newInstance(scraperEngineProvider.get());
  }

  public static BrowserInterceptor_Factory create(Provider<ScraperEngine> scraperEngineProvider) {
    return new BrowserInterceptor_Factory(scraperEngineProvider);
  }

  public static BrowserInterceptor newInstance(ScraperEngine scraperEngine) {
    return new BrowserInterceptor(scraperEngine);
  }
}
