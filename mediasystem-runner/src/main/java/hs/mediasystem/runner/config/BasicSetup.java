package hs.mediasystem.runner.config;

import hs.ddif.core.Injector;
import hs.ddif.core.JustInTimeDiscoveryPolicy;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.mediasystem.util.ini.Ini;
import hs.mediasystem.util.ini.Section;

import java.io.File;
import java.util.List;

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
          String value = section.get(key);

          if(value.matches("-?[0-9]+")) {
            injector.registerInstance(new LongProvider(Long.valueOf(value)), AnnotationDescriptor.named(section.getName() + "." + key));
          }
          else {
            if((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
              value = value.substring(1, value.length() - 1);
            }

            injector.registerInstance(new StringProvider(value), AnnotationDescriptor.named(section.getName() + "." + key));
          }
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

  private static class LongProvider implements Provider<Long> {
    private final Long value;

    public LongProvider(Long value) {
      this.value = value;
    }

    @Override
    public Long get() {
      return value;
    }
  }
}
