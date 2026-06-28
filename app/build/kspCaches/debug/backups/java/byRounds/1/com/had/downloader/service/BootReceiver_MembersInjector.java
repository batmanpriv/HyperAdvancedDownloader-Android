package com.had.downloader.service;

import com.had.downloader.data.repository.DownloadDao;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class BootReceiver_MembersInjector implements MembersInjector<BootReceiver> {
  private final Provider<DownloadDao> daoProvider;

  public BootReceiver_MembersInjector(Provider<DownloadDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  public static MembersInjector<BootReceiver> create(Provider<DownloadDao> daoProvider) {
    return new BootReceiver_MembersInjector(daoProvider);
  }

  @Override
  public void injectMembers(BootReceiver instance) {
    injectDao(instance, daoProvider.get());
  }

  @InjectedFieldSignature("com.had.downloader.service.BootReceiver.dao")
  public static void injectDao(BootReceiver instance, DownloadDao dao) {
    instance.dao = dao;
  }
}
