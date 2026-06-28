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
public final class TriggerReceiver_MembersInjector implements MembersInjector<TriggerReceiver> {
  private final Provider<DownloadDao> daoProvider;

  public TriggerReceiver_MembersInjector(Provider<DownloadDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  public static MembersInjector<TriggerReceiver> create(Provider<DownloadDao> daoProvider) {
    return new TriggerReceiver_MembersInjector(daoProvider);
  }

  @Override
  public void injectMembers(TriggerReceiver instance) {
    injectDao(instance, daoProvider.get());
  }

  @InjectedFieldSignature("com.had.downloader.service.TriggerReceiver.dao")
  public static void injectDao(TriggerReceiver instance, DownloadDao dao) {
    instance.dao = dao;
  }
}
