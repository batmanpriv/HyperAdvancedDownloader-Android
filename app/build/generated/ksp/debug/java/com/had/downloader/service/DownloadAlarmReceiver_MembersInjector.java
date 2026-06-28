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
public final class DownloadAlarmReceiver_MembersInjector implements MembersInjector<DownloadAlarmReceiver> {
  private final Provider<DownloadDao> daoProvider;

  public DownloadAlarmReceiver_MembersInjector(Provider<DownloadDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  public static MembersInjector<DownloadAlarmReceiver> create(Provider<DownloadDao> daoProvider) {
    return new DownloadAlarmReceiver_MembersInjector(daoProvider);
  }

  @Override
  public void injectMembers(DownloadAlarmReceiver instance) {
    injectDao(instance, daoProvider.get());
  }

  @InjectedFieldSignature("com.had.downloader.service.DownloadAlarmReceiver.dao")
  public static void injectDao(DownloadAlarmReceiver instance, DownloadDao dao) {
    instance.dao = dao;
  }
}
