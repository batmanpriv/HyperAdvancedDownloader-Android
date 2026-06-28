package com.had.downloader;

import com.had.downloader.service.SmartRetryEngine;
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
public final class AppModule_ProvideSmartRetryEngineFactory implements Factory<SmartRetryEngine> {
  @Override
  public SmartRetryEngine get() {
    return provideSmartRetryEngine();
  }

  public static AppModule_ProvideSmartRetryEngineFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SmartRetryEngine provideSmartRetryEngine() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSmartRetryEngine());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideSmartRetryEngineFactory INSTANCE = new AppModule_ProvideSmartRetryEngineFactory();
  }
}
