package hs.mediasystem.db;

import hs.ddif.core.Injector;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.store.BeanDefinitionStore;
import hs.ddif.plugins.ComponentScanner;

public class ServiceRunner {

  public static void start(Injector injector) throws BeanResolutionException {
    startWithoutPlugins(injector);

    injector.getInstance(PluginInitializer.class);  // Triggers plugin initialization
  }

  public static void startWithoutPlugins(Injector injector) throws BeanResolutionException {
    BeanDefinitionStore store = injector.getStore();

    ComponentScanner.scan(
      store,
      "hs.mediasystem.db",
      "hs.mediasystem.util",
      "hs.mediasystem.mediamanager"
        );

    injector.getInstance(ResponseCacheInitializer.class);  // Triggers response cache setup
  }
}
