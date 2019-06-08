package hs.mediasystem.runner.config;

import hs.ddif.core.Injector;
import hs.ddif.core.JustInTimeDiscoveryPolicy;
import hs.mediasystem.util.ini.Ini;

import java.io.File;

public class BasicSetup {
  public static Injector create() {
    Injector injector = new Injector(new JustInTimeDiscoveryPolicy());
    Ini ini = new Ini(new File("mediasystem.ini"));

    injector.registerInstance(ini);
    injector.registerInstance(injector);  // Register injector with itself

    return injector;
  }
}
