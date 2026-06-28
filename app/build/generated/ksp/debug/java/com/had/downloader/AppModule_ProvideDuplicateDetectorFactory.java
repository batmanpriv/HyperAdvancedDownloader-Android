package com.had.downloader;

import com.had.downloader.data.repository.DownloadDao;
import com.had.downloader.service.DuplicateDetector;
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
public final class AppModule_ProvideDuplicateDetectorFactory implements Factory<DuplicateDetector> {
  private final Provider<DownloadDao> daoProvider;

  public AppModule_ProvideDuplicateDetectorFactory(Provider<DownloadDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public DuplicateDetector get() {
    return provideDuplicateDetector(daoProvider.get());
  }

  public static AppModule_ProvideDuplicateDetectorFactory create(
      Provider<DownloadDao> daoProvider) {
    return new AppModule_ProvideDuplicateDetectorFactory(daoProvider);
  }

  public static DuplicateDetector provideDuplicateDetector(DownloadDao dao) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDuplicateDetector(dao));
  }
}
