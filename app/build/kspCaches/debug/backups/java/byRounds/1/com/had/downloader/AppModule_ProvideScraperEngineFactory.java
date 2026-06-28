package com.had.downloader;

import com.had.downloader.service.ScraperEngine;
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
public final class AppModule_ProvideScraperEngineFactory implements Factory<ScraperEngine> {
  @Override
  public ScraperEngine get() {
    return provideScraperEngine();
  }

  public static AppModule_ProvideScraperEngineFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ScraperEngine provideScraperEngine() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideScraperEngine());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideScraperEngineFactory INSTANCE = new AppModule_ProvideScraperEngineFactory();
  }
}
