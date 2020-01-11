package hs.mediasystem.local.client;

import hs.ddif.core.Injector;
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
import hs.mediasystem.util.ini.Ini;
import hs.mediasystem.util.ini.Section;

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

public class FrontEndRunner extends Application {
  private static final Logger LOGGER = Logger.getLogger(FrontEndRunner.class.getName());
//  private static final Comparator<PlayerFactory> PLAYER_FACTORY_COMPARATOR = (o1, o2) -> o1.getName().compareTo(o2.getName());

  public static void main(String[] args) {
    //System.setProperty("http.proxyHost", "127.0.0.1");
    //System.setProperty("http.proxyPort", "8080");
    //System.setProperty("prism.text", "t2k");  // With Segoe UI will result in pixel jumping letters with numbers that change, ie: "1:15 / 1:43:34" May render slightly differently with different spacing in the last fixed part!
    System.setProperty("prism.lcdtext", "false");
    System.setProperty("com.sun.javafx.twoLevelFocus", "true");
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
    Ini ini = injector.getInstance(Ini.class);

    Section generalSection = ini.getSection("general");
    int screenNumber = generalSection == null ? 0 : Integer.parseInt(generalSection.getDefault("screen", "0"));
    boolean alwaysOnTop = generalSection == null ? true : Boolean.parseBoolean(generalSection.getDefault("alwaysOnTop", "true"));
    SceneManager sceneManager = new FXSceneManager("MediaSystem", screenNumber, alwaysOnTop);

    injector.registerInstance(sceneManager);

    loadPlayerPlugins(injector);

    ServiceConfigurer.configure(injector);

    Annotations.initialize();

    new PluginManager(injector).loadPluginAndScan("hs.mediasystem");

    StartupPresentationProvider provider = injector.getInstance(StartupPresentationProvider.class);

    injector.getInstance(RootPresentationHandler.class);

    sceneManager.display();
    sceneManager.getRootPane().fireEvent(NavigateEvent.to(provider.get()));
  }

  private static void loadPlayerPlugins(Injector injector) throws IOException {
    PluginManager pluginManager = new PluginManager(injector);

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
