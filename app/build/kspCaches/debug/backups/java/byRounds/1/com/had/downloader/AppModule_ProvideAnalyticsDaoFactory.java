package com.had.downloader;

import com.had.downloader.data.repository.AnalyticsDao;
import com.had.downloader.data.repository.HADDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideAnalyticsDaoFactory implements Factory<AnalyticsDao> {
  private final Provider<HADDatabase> dbProvider;

  public AppModule_ProvideAnalyticsDaoFactory(Provider<HADDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public AnalyticsDao get() {
    return provideAnalyticsDao(dbProvider.get());
  }

  public static AppModule_ProvideAnalyticsDaoFactory create(Provider<HADDatabase> dbProvider) {
    return new AppModule_ProvideAnalyticsDaoFactory(dbProvider);
  }

  public static AnalyticsDao provideAnalyticsDao(HADDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAnalyticsDao(db));
  }
}
