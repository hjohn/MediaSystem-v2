package hs.mediasystem.runner;

import hs.ddif.core.Injector;
import hs.ddif.plugins.PluginManager;
import hs.mediasystem.plugin.rootmenu.StartupLocationSetting;
import hs.mediasystem.runner.config.BasicSetup;
import hs.mediasystem.runner.config.DatabaseConfigurer;
import hs.mediasystem.runner.config.LoggingConfigurer;
import hs.mediasystem.runner.config.MediaSystemConfigurer;
import hs.mediasystem.runner.util.DefaultSceneManager;
import hs.mediasystem.runner.util.SceneManager;
import hs.mediasystem.util.ini.Ini;
import hs.mediasystem.util.ini.Section;

import java.util.logging.Level;
import java.util.logging.Logger;

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
    LoggingConfigurer.configure();

    Injector injector = BasicSetup.create();
    Ini ini = injector.getInstance(Ini.class);

    DatabaseConfigurer.configure(injector, ini.getSection("database"));
    MediaSystemConfigurer.configure(injector);

    Section generalSection = ini.getSection("general");
    int screenNumber = generalSection == null ? 0 : Integer.parseInt(generalSection.getDefault("screen", "0"));

    SceneManager sceneManager = new DefaultSceneManager("MediaSystem", screenNumber);

    injector.registerInstance(sceneManager);

    new PluginManager(injector).loadPluginAndScan("hs.mediasystem");

    StartupLocationSetting startupLocationSetting = injector.getInstance(StartupLocationSetting.class);

    injector.getInstance(RootPresentationHandler.class);

    sceneManager.getRootPane().fireEvent(NavigateEvent.to(startupLocationSetting.get()));
  }
}
