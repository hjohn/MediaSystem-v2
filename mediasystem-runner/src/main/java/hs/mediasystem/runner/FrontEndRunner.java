package hs.mediasystem.runner;

import hs.ddif.core.Injector;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.Value;
import hs.ddif.plugins.PluginManager;
import hs.mediasystem.runner.config.BasicSetup;
import hs.mediasystem.runner.config.DatabaseConfigurer;
import hs.mediasystem.runner.config.LoggingConfigurer;
import hs.mediasystem.runner.config.MediaSystemConfigurer;
import hs.mediasystem.runner.util.FXSceneManager;
import hs.mediasystem.runner.util.SceneManager;
import hs.mediasystem.util.ini.Ini;
import hs.mediasystem.util.ini.Section;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.stage.Stage;

import javax.inject.Named;
import javax.inject.Provider;

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

  @Override
  public void start(Stage primaryStage) throws Exception {
    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

    LoggingConfigurer.configure();

    Injector injector = BasicSetup.create();
    Ini ini = injector.getInstance(Ini.class);

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

    Section generalSection = ini.getSection("general");
    int screenNumber = generalSection == null ? 0 : Integer.parseInt(generalSection.getDefault("screen", "0"));
    boolean alwaysOnTop = generalSection == null ? true : Boolean.parseBoolean(generalSection.getDefault("alwaysOnTop", "true"));
    SceneManager sceneManager = new FXSceneManager("MediaSystem", screenNumber, alwaysOnTop);

    injector.registerInstance(sceneManager);

    DatabaseConfigurer.configure(injector, ini.getSection("database"));
    MediaSystemConfigurer.configure(injector);

    new PluginManager(injector).loadPluginAndScan("hs.mediasystem");

    StartupPresentationProvider provider = injector.getInstance(StartupPresentationProvider.class);

    injector.getInstance(RootPresentationHandler.class);

    sceneManager.display();
    sceneManager.getRootPane().fireEvent(NavigateEvent.to(provider.get()));
  }
}
