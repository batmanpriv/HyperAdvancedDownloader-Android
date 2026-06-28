package com.had.downloader;

import com.had.downloader.service.VideoDetectionEngine;
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
public final class AppModule_ProvideVideoDetectionEngineFactory implements Factory<VideoDetectionEngine> {
  @Override
  public VideoDetectionEngine get() {
    return provideVideoDetectionEngine();
  }

  public static AppModule_ProvideVideoDetectionEngineFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static VideoDetectionEngine provideVideoDetectionEngine() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideVideoDetectionEngine());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideVideoDetectionEngineFactory INSTANCE = new AppModule_ProvideVideoDetectionEngineFactory();
  }
}
