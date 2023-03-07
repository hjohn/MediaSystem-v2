package hs.mediasystem.runner;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.util.exception.Throwables;

import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.int4.dirk.api.InstanceResolver;

@Singleton
public class StartupPresentationProvider implements Supplier<Presentation> {
  private static final Logger LOGGER = Logger.getLogger(StartupPresentationProvider.class.getName());
  private static final String FALLBACK_PRESENTATION_FACTORY = "hs.mediasystem.plugin.home.HomePresentation$Factory";

  public interface Plugin {
    Presentation create();
  }

  @Inject private InstanceResolver instanceResolver;
  @Inject @Named("general.startup-presentation-plugin") @Nullable private String startupPresentationPluginClassName;

  @Override
  public Presentation get() {
    if(startupPresentationPluginClassName != null) {
      try {
        return createPlugin(startupPresentationPluginClassName);
      }
      catch(Exception e) {
        LOGGER.warning("Unable to load startup plugin: " + startupPresentationPluginClassName + "; falling back to default: " + Throwables.formatAsOneLine(e));
      }
    }

    try {
      return createPlugin(FALLBACK_PRESENTATION_FACTORY);
    }
    catch(Exception e) {
      throw new IllegalStateException("Unable to load startup plugin: " + startupPresentationPluginClassName + " and fallback to: " + FALLBACK_PRESENTATION_FACTORY + " also failed");
    }
  }

  private Presentation createPlugin(String className) throws ClassNotFoundException {
    Class<?> cls = Class.forName(className);
    Plugin plugin = (Plugin)instanceResolver.getInstance(cls);

    return plugin.create();
  }
}
