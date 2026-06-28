package com.had.downloader;

import com.had.downloader.service.RemoteDownloadServer;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideRemoteDownloadServerFactory implements Factory<RemoteDownloadServer> {
  @Override
  public RemoteDownloadServer get() {
    return provideRemoteDownloadServer();
  }

  public static AppModule_ProvideRemoteDownloadServerFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RemoteDownloadServer provideRemoteDownloadServer() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideRemoteDownloadServer());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideRemoteDownloadServerFactory INSTANCE = new AppModule_ProvideRemoteDownloadServerFactory();
  }
}
