package com.had.downloader;

import com.had.downloader.data.repository.AnalyticsDao;
import com.had.downloader.data.repository.AnalyticsRepository;
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
public final class AppModule_ProvideAnalyticsRepositoryFactory implements Factory<AnalyticsRepository> {
  private final Provider<AnalyticsDao> daoProvider;

  public AppModule_ProvideAnalyticsRepositoryFactory(Provider<AnalyticsDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public AnalyticsRepository get() {
    return provideAnalyticsRepository(daoProvider.get());
  }

  public static AppModule_ProvideAnalyticsRepositoryFactory create(
      Provider<AnalyticsDao> daoProvider) {
    return new AppModule_ProvideAnalyticsRepositoryFactory(daoProvider);
  }

  public static AnalyticsRepository provideAnalyticsRepository(AnalyticsDao dao) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAnalyticsRepository(dao));
  }
}
