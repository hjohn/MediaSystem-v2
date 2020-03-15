package hs.mediasystem.db;

import hs.ddif.core.Injector;
import hs.ddif.core.inject.store.BeanDefinitionStore;
import hs.ddif.plugins.PluginManager;

public class ServiceRunner {

  public static void start(Injector injector) {
    BeanDefinitionStore store = injector.getStore();

    PluginManager pluginManager = new PluginManager(store);

    pluginManager.loadPluginAndScan("hs.mediasystem.db", "hs.mediasystem.util", "hs.mediasystem.mediamanager");
  }
}
