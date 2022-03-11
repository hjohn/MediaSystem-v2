package hs.mediasystem.db;

import hs.ddif.core.Injector;
import hs.ddif.core.api.CandidateRegistry;
import hs.ddif.plugins.ComponentScanner;
import hs.ddif.plugins.ComponentScannerFactory;

public class ServiceRunner {

  public static void start(Injector injector) {
    CandidateRegistry registry = injector.getCandidateRegistry();

    ComponentScannerFactory componentScannerFactory = injector.getInstance(ComponentScannerFactory.class);
    ComponentScanner componentScanner = componentScannerFactory.create(
      "hs.mediasystem.db",
      "hs.mediasystem.util",
      "hs.mediasystem.mediamanager"
    );

    componentScanner.scan(registry);

    injector.getInstance(ResponseCacheInitializer.class);  // Triggers response cache setup
    injector.getInstance(PluginInitializer.class);  // Triggers plugin initialisation
  }
}
