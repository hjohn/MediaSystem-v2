package hs.mediasystem.db;

import hs.ddif.core.Injector;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.store.BeanDefinitionStore;
import hs.ddif.plugins.PluginManager;

public class ServiceRunner {

  public static void start(Injector injector) throws BeanResolutionException {
    BeanDefinitionStore store = injector.getStore();

    PluginManager pluginManager = new PluginManager(store);

    pluginManager.loadPluginAndScan("hs.mediasystem.db", "hs.mediasystem.util", "hs.mediasystem.mediamanager");

    injector.getInstance(PluginInitializer.class);  // Triggers plugin initialization
  }
}
