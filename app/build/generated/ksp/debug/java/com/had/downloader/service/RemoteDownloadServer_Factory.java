package com.had.downloader.service;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class RemoteDownloadServer_Factory implements Factory<RemoteDownloadServer> {
  @Override
  public RemoteDownloadServer get() {
    return newInstance();
  }

  public static RemoteDownloadServer_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RemoteDownloadServer newInstance() {
    return new RemoteDownloadServer();
  }

  private static final class InstanceHolder {
    private static final RemoteDownloadServer_Factory INSTANCE = new RemoteDownloadServer_Factory();
  }
}
