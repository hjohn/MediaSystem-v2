package hs.mediasystem.runner.config;

import hs.ddif.core.Injector;
import hs.ddif.core.JustInTimeDiscoveryPolicy;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.Value;
import hs.mediasystem.util.ini.Ini;
import hs.mediasystem.util.ini.Section;

import java.io.File;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

public class BasicSetup {
  public static Injector create() {
    Injector injector = new Injector(new JustInTimeDiscoveryPolicy());
    Ini ini = new Ini(new File("mediasystem.ini"));

    injector.registerInstance(ini);
    injector.registerInstance(injector);  // Register injector with itself

    /*
     * Add INI fields to Injector
     */

    for(List<Section> sections : ini) {
      for(Section section : sections) {
        for(String key : section) {
          injector.register(new StringProvider(section.get(key)), AnnotationDescriptor.describe(Named.class, new Value("value", section.getName() + "." + key)));
        }
      }
    }

    return injector;
  }

  private static class StringProvider implements Provider<String> {
    private final String value;

    public StringProvider(String value) {
      this.value = value;
    }

    @Override
    public String get() {
      return value;
    }
  }
}
