package com.had.downloader;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.had.downloader.data.repository.AnalyticsDao;
import com.had.downloader.data.repository.AnalyticsRepository;
import com.had.downloader.data.repository.DownloadDao;
import com.had.downloader.data.repository.HADDatabase;
import com.had.downloader.service.BootReceiver;
import com.had.downloader.service.BootReceiver_MembersInjector;
import com.had.downloader.service.ClipboardMonitor;
import com.had.downloader.service.DownloadAlarmReceiver;
import com.had.downloader.service.DownloadAlarmReceiver_MembersInjector;
import com.had.downloader.service.DownloadSchedulerService;
import com.had.downloader.service.DownloadSchedulerService_MembersInjector;
import com.had.downloader.service.DuplicateDetector;
import com.had.downloader.service.FileSizeFetcher;
import com.had.downloader.service.ForegroundDownloadService;
import com.had.downloader.service.ForegroundDownloadService_MembersInjector;
import com.had.downloader.service.HlsDownloader;
import com.had.downloader.service.RemoteDownloadServer;
import com.had.downloader.service.ScraperEngine;
import com.had.downloader.service.SmartDownloader;
import com.had.downloader.service.TriggerReceiver;
import com.had.downloader.service.TriggerReceiver_MembersInjector;
import com.had.downloader.service.WebArchiveEngine;
import com.had.downloader.ui.screens.MainViewModel;
import com.had.downloader.ui.screens.MainViewModel_HiltModules;
import com.had.downloader.ui.screens.ShareReceiverActivity;
import com.had.downloader.ui.screens.ShareReceiverActivity_MembersInjector;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideApplicationFactory;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerHadApplication_HiltComponents_SingletonC {
  private DaggerHadApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public HadApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements HadApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public HadApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements HadApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public HadApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements HadApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public HadApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements HadApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public HadApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements HadApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public HadApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements HadApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public HadApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements HadApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public HadApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends HadApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends HadApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends HadApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends HadApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public void injectShareReceiverActivity(ShareReceiverActivity shareReceiverActivity) {
      injectShareReceiverActivity2(shareReceiverActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(Collections.<String, Boolean>singletonMap(LazyClassKeyProvider.com_had_downloader_ui_screens_MainViewModel, MainViewModel_HiltModules.KeyModule.provide()));
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @CanIgnoreReturnValue
    private ShareReceiverActivity injectShareReceiverActivity2(ShareReceiverActivity instance) {
      ShareReceiverActivity_MembersInjector.injectHlsDetector(instance, singletonCImpl.provideHlsDownloaderProvider.get());
      return instance;
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_had_downloader_ui_screens_MainViewModel = "com.had.downloader.ui.screens.MainViewModel";

      @KeepFieldType
      MainViewModel com_had_downloader_ui_screens_MainViewModel2;
    }
  }

  private static final class ViewModelCImpl extends HadApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<MainViewModel> mainViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.mainViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(Collections.<String, javax.inject.Provider<ViewModel>>singletonMap(LazyClassKeyProvider.com_had_downloader_ui_screens_MainViewModel, ((Provider) mainViewModelProvider)));
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_had_downloader_ui_screens_MainViewModel = "com.had.downloader.ui.screens.MainViewModel";

      @KeepFieldType
      MainViewModel com_had_downloader_ui_screens_MainViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.had.downloader.ui.screens.MainViewModel 
          return (T) new MainViewModel(ApplicationContextModule_ProvideApplicationFactory.provideApplication(singletonCImpl.applicationContextModule), singletonCImpl.provideDaoProvider.get(), singletonCImpl.provideSmartDownloaderProvider.get(), singletonCImpl.provideHlsDownloaderProvider.get(), singletonCImpl.provideScraperEngineProvider.get(), singletonCImpl.provideAnalyticsRepositoryProvider.get(), singletonCImpl.provideRemoteDownloadServerProvider.get(), singletonCImpl.provideClipboardMonitorProvider.get(), singletonCImpl.provideDuplicateDetectorProvider.get(), singletonCImpl.webArchiveEngineProvider.get(), singletonCImpl.provideFileSizeFetcherProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends HadApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends HadApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectDownloadSchedulerService(DownloadSchedulerService downloadSchedulerService) {
      injectDownloadSchedulerService2(downloadSchedulerService);
    }

    @Override
    public void injectForegroundDownloadService(
        ForegroundDownloadService foregroundDownloadService) {
      injectForegroundDownloadService2(foregroundDownloadService);
    }

    @CanIgnoreReturnValue
    private DownloadSchedulerService injectDownloadSchedulerService2(
        DownloadSchedulerService instance) {
      DownloadSchedulerService_MembersInjector.injectSmartDownloader(instance, singletonCImpl.provideSmartDownloaderProvider.get());
      DownloadSchedulerService_MembersInjector.injectHlsDownloader(instance, singletonCImpl.provideHlsDownloaderProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private ForegroundDownloadService injectForegroundDownloadService2(
        ForegroundDownloadService instance) {
      ForegroundDownloadService_MembersInjector.injectDao(instance, singletonCImpl.provideDaoProvider.get());
      ForegroundDownloadService_MembersInjector.injectSmartDownloader(instance, singletonCImpl.provideSmartDownloaderProvider.get());
      ForegroundDownloadService_MembersInjector.injectHlsDownloader(instance, singletonCImpl.provideHlsDownloaderProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends HadApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<HADDatabase> provideDatabaseProvider;

    private Provider<DownloadDao> provideDaoProvider;

    private Provider<HlsDownloader> provideHlsDownloaderProvider;

    private Provider<SmartDownloader> provideSmartDownloaderProvider;

    private Provider<ScraperEngine> provideScraperEngineProvider;

    private Provider<AnalyticsDao> provideAnalyticsDaoProvider;

    private Provider<AnalyticsRepository> provideAnalyticsRepositoryProvider;

    private Provider<RemoteDownloadServer> provideRemoteDownloadServerProvider;

    private Provider<ClipboardMonitor> provideClipboardMonitorProvider;

    private Provider<DuplicateDetector> provideDuplicateDetectorProvider;

    private Provider<WebArchiveEngine> webArchiveEngineProvider;

    private Provider<FileSizeFetcher> provideFileSizeFetcherProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<HADDatabase>(singletonCImpl, 1));
      this.provideDaoProvider = DoubleCheck.provider(new SwitchingProvider<DownloadDao>(singletonCImpl, 0));
      this.provideHlsDownloaderProvider = DoubleCheck.provider(new SwitchingProvider<HlsDownloader>(singletonCImpl, 2));
      this.provideSmartDownloaderProvider = DoubleCheck.provider(new SwitchingProvider<SmartDownloader>(singletonCImpl, 3));
      this.provideScraperEngineProvider = DoubleCheck.provider(new SwitchingProvider<ScraperEngine>(singletonCImpl, 4));
      this.provideAnalyticsDaoProvider = DoubleCheck.provider(new SwitchingProvider<AnalyticsDao>(singletonCImpl, 6));
      this.provideAnalyticsRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<AnalyticsRepository>(singletonCImpl, 5));
      this.provideRemoteDownloadServerProvider = DoubleCheck.provider(new SwitchingProvider<RemoteDownloadServer>(singletonCImpl, 7));
      this.provideClipboardMonitorProvider = DoubleCheck.provider(new SwitchingProvider<ClipboardMonitor>(singletonCImpl, 8));
      this.provideDuplicateDetectorProvider = DoubleCheck.provider(new SwitchingProvider<DuplicateDetector>(singletonCImpl, 9));
      this.webArchiveEngineProvider = DoubleCheck.provider(new SwitchingProvider<WebArchiveEngine>(singletonCImpl, 10));
      this.provideFileSizeFetcherProvider = DoubleCheck.provider(new SwitchingProvider<FileSizeFetcher>(singletonCImpl, 11));
    }

    @Override
    public void injectHadApplication(HadApplication hadApplication) {
    }

    @Override
    public void injectBootReceiver(BootReceiver bootReceiver) {
      injectBootReceiver2(bootReceiver);
    }

    @Override
    public void injectDownloadAlarmReceiver(DownloadAlarmReceiver downloadAlarmReceiver) {
      injectDownloadAlarmReceiver2(downloadAlarmReceiver);
    }

    @Override
    public void injectTriggerReceiver(TriggerReceiver triggerReceiver) {
      injectTriggerReceiver2(triggerReceiver);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    @CanIgnoreReturnValue
    private BootReceiver injectBootReceiver2(BootReceiver instance) {
      BootReceiver_MembersInjector.injectDao(instance, provideDaoProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private DownloadAlarmReceiver injectDownloadAlarmReceiver2(DownloadAlarmReceiver instance) {
      DownloadAlarmReceiver_MembersInjector.injectDao(instance, provideDaoProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private TriggerReceiver injectTriggerReceiver2(TriggerReceiver instance) {
      TriggerReceiver_MembersInjector.injectDao(instance, provideDaoProvider.get());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.had.downloader.data.repository.DownloadDao 
          return (T) AppModule_ProvideDaoFactory.provideDao(singletonCImpl.provideDatabaseProvider.get());

          case 1: // com.had.downloader.data.repository.HADDatabase 
          return (T) AppModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.had.downloader.service.HlsDownloader 
          return (T) AppModule_ProvideHlsDownloaderFactory.provideHlsDownloader();

          case 3: // com.had.downloader.service.SmartDownloader 
          return (T) AppModule_ProvideSmartDownloaderFactory.provideSmartDownloader();

          case 4: // com.had.downloader.service.ScraperEngine 
          return (T) AppModule_ProvideScraperEngineFactory.provideScraperEngine();

          case 5: // com.had.downloader.data.repository.AnalyticsRepository 
          return (T) AppModule_ProvideAnalyticsRepositoryFactory.provideAnalyticsRepository(singletonCImpl.provideAnalyticsDaoProvider.get());

          case 6: // com.had.downloader.data.repository.AnalyticsDao 
          return (T) AppModule_ProvideAnalyticsDaoFactory.provideAnalyticsDao(singletonCImpl.provideDatabaseProvider.get());

          case 7: // com.had.downloader.service.RemoteDownloadServer 
          return (T) AppModule_ProvideRemoteDownloadServerFactory.provideRemoteDownloadServer();

          case 8: // com.had.downloader.service.ClipboardMonitor 
          return (T) AppModule_ProvideClipboardMonitorFactory.provideClipboardMonitor();

          case 9: // com.had.downloader.service.DuplicateDetector 
          return (T) AppModule_ProvideDuplicateDetectorFactory.provideDuplicateDetector(singletonCImpl.provideDaoProvider.get());

          case 10: // com.had.downloader.service.WebArchiveEngine 
          return (T) new WebArchiveEngine();

          case 11: // com.had.downloader.service.FileSizeFetcher 
          return (T) AppModule_ProvideFileSizeFetcherFactory.provideFileSizeFetcher();

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
