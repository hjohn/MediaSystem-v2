package hs.mediasystem.local.client;

import hs.ddif.core.Injector;
import hs.ddif.core.inject.store.BeanDefinitionStore;
import hs.ddif.plugins.PluginManager;
import hs.mediasystem.db.ServiceConfigurer;
import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.runner.RootPresentationHandler;
import hs.mediasystem.runner.StartupPresentationProvider;
import hs.mediasystem.runner.config.BasicSetup;
import hs.mediasystem.runner.config.LoggingConfigurer;
import hs.mediasystem.runner.expose.Annotations;
import hs.mediasystem.runner.util.FXSceneManager;
import hs.mediasystem.runner.util.SceneManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.stage.Stage;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

public class FrontEndRunner extends Application {
  private static final Logger LOGGER = Logger.getLogger(FrontEndRunner.class.getName());

  public static void main(String[] args) {
    //System.setProperty("prism.text", "t2k");  // With Segoe UI will result in pixel jumping letters with numbers that change, ie: "1:15 / 1:43:34" May render slightly differently with different spacing in the last fixed part!
    System.setProperty("prism.lcdtext", "false");
    //System.setProperty("com.sun.javafx.twoLevelFocus", "true");
    //System.setProperty("javafx.animation.pulse", "2");
    //System.setProperty("javafx.pulseLogger", "true");

    Application.launch(args);
  }

  @Override
  public void init() throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        LOGGER.log(Level.SEVERE, "Exception in thread \"" + t.getName() + "\"", e);
      }
    });

    super.init();
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

    LoggingConfigurer.configure();

    Injector injector = BasicSetup.create();
    FXSceneManagerConfiguration configuration = injector.getInstance(FXSceneManagerConfiguration.class);

    LOGGER.info("Creating Scene on screen " + configuration.screenNumber + " with always on top set to " + configuration.alwaysOnTop);

    SceneManager sceneManager = new FXSceneManager("MediaSystem", configuration.screenNumber.intValue(), configuration.alwaysOnTop);

    injector.registerInstance(sceneManager);

    loadPlayerPlugins(injector.getStore());

    injector.getInstance(ServiceConfigurer.class);  // Triggers configuration of service layer

    Annotations.initialize();

    new PluginManager(injector.getStore()).loadPluginAndScan("hs.mediasystem");

    StartupPresentationProvider provider = injector.getInstance(StartupPresentationProvider.class);

    injector.getInstance(RootPresentationHandler.class);

    sceneManager.display();
    sceneManager.getRootPane().fireEvent(NavigateEvent.to(provider.get()));
  }

  public static class FXSceneManagerConfiguration {
    @Inject @Nullable @Named("general.screen") public Long screenNumber = 0L;
    @Inject @Nullable @Named("general.alwaysOnTop") public Boolean alwaysOnTop = false;
  }

  private static void loadPlayerPlugins(BeanDefinitionStore store) throws IOException {
    PluginManager pluginManager = new PluginManager(store);

    Path root = Paths.get("ui-plugins");

    if(Files.exists(root)) {
      Files.find(root, 1, (p, a) -> !p.equals(root)).forEach(p -> {
        try {
          List<URL> urls = Files.find(p, 1, (cp, a) -> !cp.equals(p)).map(Path::toUri).map(uri -> {
            try {
              return uri.toURL();
            }
            catch(MalformedURLException e) {
              throw new IllegalStateException(e);
            }
          }).collect(Collectors.toList());

          pluginManager.loadPluginAndScan(urls.toArray(new URL[] {}));
        }
        catch(IOException e) {
          throw new IllegalStateException(e);
        }
      });
    }
    else {
      pluginManager.loadPluginAndScan(
        URI.create("file:/P:/Dev/git/MediaSystem-v2/mediasystem-ext-vlc/target/classes/").toURL(),
        new URL("file:P:/Dev/git/MediaSystem-v2/mediasystem-ext-vlc/target/dependencies-only.jar")
      );
      pluginManager.loadPluginAndScan(
        URI.create("file:/P:/Dev/git/MediaSystem-v2/mediasystem-ext-mpv/target/classes/").toURL(),
        new URL("file:P:/Dev/git/MediaSystem-v2/mediasystem-ext-mpv/target/dependencies-only.jar")
      );
    }
  }
}
