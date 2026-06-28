package com.had.downloader.service;

import com.had.downloader.data.repository.DownloadDao;
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
public final class DuplicateDetector_Factory implements Factory<DuplicateDetector> {
  private final Provider<DownloadDao> daoProvider;

  public DuplicateDetector_Factory(Provider<DownloadDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public DuplicateDetector get() {
    return newInstance(daoProvider.get());
  }

  public static DuplicateDetector_Factory create(Provider<DownloadDao> daoProvider) {
    return new DuplicateDetector_Factory(daoProvider);
  }

  public static DuplicateDetector newInstance(DownloadDao dao) {
    return new DuplicateDetector(dao);
  }
}
