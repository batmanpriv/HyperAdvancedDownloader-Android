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
public final class VideoDetectionEngine_Factory implements Factory<VideoDetectionEngine> {
  @Override
  public VideoDetectionEngine get() {
    return newInstance();
  }

  public static VideoDetectionEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static VideoDetectionEngine newInstance() {
    return new VideoDetectionEngine();
  }

  private static final class InstanceHolder {
    private static final VideoDetectionEngine_Factory INSTANCE = new VideoDetectionEngine_Factory();
  }
}
