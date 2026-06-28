package com.had.downloader.ui.screens;

import com.had.downloader.service.HlsDownloader;
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
public final class ShareReceiverActivity_MembersInjector implements MembersInjector<ShareReceiverActivity> {
  private final Provider<HlsDownloader> hlsDetectorProvider;

  public ShareReceiverActivity_MembersInjector(Provider<HlsDownloader> hlsDetectorProvider) {
    this.hlsDetectorProvider = hlsDetectorProvider;
  }

  public static MembersInjector<ShareReceiverActivity> create(
      Provider<HlsDownloader> hlsDetectorProvider) {
    return new ShareReceiverActivity_MembersInjector(hlsDetectorProvider);
  }

  @Override
  public void injectMembers(ShareReceiverActivity instance) {
    injectHlsDetector(instance, hlsDetectorProvider.get());
  }

  @InjectedFieldSignature("com.had.downloader.ui.screens.ShareReceiverActivity.hlsDetector")
  public static void injectHlsDetector(ShareReceiverActivity instance, HlsDownloader hlsDetector) {
    instance.hlsDetector = hlsDetector;
  }
}
