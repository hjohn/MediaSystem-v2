package hs.mediasystem.runner;

import hs.ddif.core.inject.instantiator.Instantiator;
import hs.mediasystem.plugin.rootmenu.MenuPresentation;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.util.Throwables;

import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class StartupPresentationProvider implements Supplier<Presentation> {
  private static final Logger LOGGER = Logger.getLogger(StartupPresentationProvider.class.getName());

  public interface Plugin {
    Presentation create();
  }

  @Inject private MenuPresentation.Factory factory;
  @Inject private Instantiator instantiator;
  @Inject @Named("general.startup-presentation-plugin") @Nullable private String startupPresentationPluginClassName;

  @Override
  public Presentation get() {
    if(startupPresentationPluginClassName != null) {
      try {
        Class<?> cls = Class.forName(startupPresentationPluginClassName);
        Plugin plugin = (Plugin)instantiator.getInstance(cls);

        return plugin.create();
      }
      catch(Exception e) {
        LOGGER.warning("Unable to load startup plugin: " + startupPresentationPluginClassName + "; falling back to default: " + Throwables.formatAsOneLine(e));
      }
    }

    return factory.create();
  }
}
