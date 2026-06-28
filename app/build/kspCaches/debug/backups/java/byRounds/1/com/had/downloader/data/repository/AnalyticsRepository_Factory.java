package com.had.downloader.data.repository;

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
public final class AnalyticsRepository_Factory implements Factory<AnalyticsRepository> {
  private final Provider<AnalyticsDao> analyticsDaoProvider;

  public AnalyticsRepository_Factory(Provider<AnalyticsDao> analyticsDaoProvider) {
    this.analyticsDaoProvider = analyticsDaoProvider;
  }

  @Override
  public AnalyticsRepository get() {
    return newInstance(analyticsDaoProvider.get());
  }

  public static AnalyticsRepository_Factory create(Provider<AnalyticsDao> analyticsDaoProvider) {
    return new AnalyticsRepository_Factory(analyticsDaoProvider);
  }

  public static AnalyticsRepository newInstance(AnalyticsDao analyticsDao) {
    return new AnalyticsRepository(analyticsDao);
  }
}
