package com.had.downloader;

import com.had.downloader.service.ClipboardMonitor;
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
public final class AppModule_ProvideClipboardMonitorFactory implements Factory<ClipboardMonitor> {
  @Override
  public ClipboardMonitor get() {
    return provideClipboardMonitor();
  }

  public static AppModule_ProvideClipboardMonitorFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ClipboardMonitor provideClipboardMonitor() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideClipboardMonitor());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideClipboardMonitorFactory INSTANCE = new AppModule_ProvideClipboardMonitorFactory();
  }
}
