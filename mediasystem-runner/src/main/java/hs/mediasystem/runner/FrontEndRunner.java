package hs.mediasystem.runner;

import hs.ddif.core.Injector;
import hs.ddif.plugins.PluginManager;
import hs.mediasystem.plugin.rootmenu.StartupLocationSetting;
import hs.mediasystem.util.ini.Ini;
import hs.mediasystem.util.ini.Section;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.stage.Stage;

public class FrontEndRunner extends Application {
  private static final Logger LOGGER = Logger.getLogger(FrontEndRunner.class.getName());
//  private static final Comparator<PlayerFactory> PLAYER_FACTORY_COMPARATOR = (o1, o2) -> o1.getName().compareTo(o2.getName());

  public static void main(String[] args) {
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
    Injector injector = MediaSystemConfigurer.start();

    Ini ini = injector.getInstance(Ini.class);
    Section generalSection = ini.getSection("general");
    int screenNumber = generalSection == null ? 0 : Integer.parseInt(generalSection.getDefault("screen", "0"));

    SceneManager sceneManager = new DuoWindowSceneManager("MediaSystem", screenNumber);

    injector.registerInstance(sceneManager);

    new PluginManager(injector).loadPluginAndScan("hs.mediasystem");

    SceneNavigator navigator = injector.getInstance(SceneNavigator.class);

    //@Inject @Named("primary") private

    StartupLocationSetting startupLocationSetting = injector.getInstance(StartupLocationSetting.class);
//    Supplier<Object> startupLocationSupplier = injector.getInstance(new TypeReference<Supplier<Object>>() {}.getType(), AnnotationDescriptor.describe(Named.class, new Value("value", "primary")));

    navigator.setHistory(Arrays.asList(startupLocationSetting.get()));

//    loadPlugins(injector);
//
//    ObjectProperty<PlayerFactory> selectedPlayerFactory = configurePlayers(injector, ini);
//
//    configureSettings(injector, selectedPlayerFactory);
//
//    ProgramController controller = injector.getInstance(ProgramController.class);
//
//    controller.showMainScreen();
  }

  private static void loadPlugins(final Injector injector) throws IOException {
    PluginManager pluginManager = new PluginManager(injector);

    Path pluginsPath = Paths.get("plugins");

    if(Files.isDirectory(pluginsPath)) {
      Files.find(pluginsPath, 10, (p, bfa) -> bfa.isRegularFile()).forEach(p -> {
        try {
          LOGGER.info("Loading plugin: " + p);
          pluginManager.loadPluginAndScan(p.toUri().toURL());
        }
        catch(Exception e) {
          throw new IllegalStateException(e);
        }
      });
    }
    else {
//      pluginManager.loadPluginAndScan(new File("../mediasystem-ext-player-vlc/target/mediasystem-ext-player-vlc-1.0.0-SNAPSHOT.jar").toURI().toURL());
//      pluginManager.loadPluginAndScan(new File("../mediasystem-ext-all/target/mediasystem-ext-all-1.0.0-SNAPSHOT.jar").toURI().toURL());
      pluginManager.loadPluginAndScan(new File("P:\\Dev\\git\\MediaSystem\\mediasystem-ext-player-vlc\\target\\classes").toURI().toURL());
      pluginManager.loadPluginAndScan(new File("P:\\Dev\\git\\MediaSystem\\mediasystem-ext-all\\target\\classes").toURI().toURL());
    }
  }
}
