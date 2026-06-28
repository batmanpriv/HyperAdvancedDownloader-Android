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
public final class SmartRetryEngine_Factory implements Factory<SmartRetryEngine> {
  @Override
  public SmartRetryEngine get() {
    return newInstance();
  }

  public static SmartRetryEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SmartRetryEngine newInstance() {
    return new SmartRetryEngine();
  }

  private static final class InstanceHolder {
    private static final SmartRetryEngine_Factory INSTANCE = new SmartRetryEngine_Factory();
  }
}
