package com.had.downloader;

import com.had.downloader.data.repository.DownloadDao;
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
public final class AppModule_ProvideDaoFactory implements Factory<DownloadDao> {
  private final Provider<HADDatabase> dbProvider;

  public AppModule_ProvideDaoFactory(Provider<HADDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public DownloadDao get() {
    return provideDao(dbProvider.get());
  }

  public static AppModule_ProvideDaoFactory create(Provider<HADDatabase> dbProvider) {
    return new AppModule_ProvideDaoFactory(dbProvider);
  }

  public static DownloadDao provideDao(HADDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDao(db));
  }
}
