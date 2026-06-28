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
public final class ClipboardMonitor_Factory implements Factory<ClipboardMonitor> {
  @Override
  public ClipboardMonitor get() {
    return newInstance();
  }

  public static ClipboardMonitor_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ClipboardMonitor newInstance() {
    return new ClipboardMonitor();
  }

  private static final class InstanceHolder {
    private static final ClipboardMonitor_Factory INSTANCE = new ClipboardMonitor_Factory();
  }
}
