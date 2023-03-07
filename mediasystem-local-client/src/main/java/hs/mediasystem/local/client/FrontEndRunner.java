package hs.mediasystem.local.client;

import hs.mediasystem.db.ServiceRunner;
import hs.mediasystem.plugin.playback.scene.PlayerSetting;
import hs.mediasystem.presentation.Theme;
import hs.mediasystem.presentation.ViewPort;
import hs.mediasystem.runner.RootPresentationHandler;
import hs.mediasystem.runner.StartupPresentationProvider;
import hs.mediasystem.runner.config.BasicSetup;
import hs.mediasystem.runner.config.LoggingConfigurer;
import hs.mediasystem.runner.expose.Annotations;
import hs.mediasystem.runner.root.RootPresentation;
import hs.mediasystem.runner.util.FXSceneManager.SceneLayout;
import hs.mediasystem.runner.util.SceneManager;
import hs.mediasystem.ui.api.player.PlayerFactory;
import hs.mediasystem.ui.api.player.PlayerFactory.IntegrationMethod;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.int4.dirk.api.CandidateRegistry;
import org.int4.dirk.api.Injector;
import org.int4.dirk.plugins.ComponentScanner;
import org.int4.dirk.plugins.ComponentScannerFactory;
import org.int4.dirk.plugins.Plugin;
import org.int4.dirk.plugins.PluginManager;

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

    ServiceRunner.start(injector);  // Triggers configuration of service layer

    Annotations.initialize();

    // TODO should try to have all these #getInstance calls work via regular injection

    ComponentScannerFactory componentScannerFactory = injector.getInstance(ComponentScannerFactory.class);
    ComponentScanner componentScanner = componentScannerFactory.create(
      "hs.mediasystem.runner",
      "hs.mediasystem.presentation",
      "hs.mediasystem.plugin",
      "hs.mediasystem.local.client.service"
    );

    componentScanner.scan(injector.getCandidateRegistry());

    loadPlayerPlugins(componentScannerFactory, injector.getCandidateRegistry());

    StartupPresentationProvider provider = injector.getInstance(StartupPresentationProvider.class);

    injector.getInstance(RootPresentationHandler.class);

    SceneManager sceneManager = injector.getInstance(SceneManager.class);
    PlayerSetting playerSetting = injector.getInstance(PlayerSetting.class);

    SceneLayout sceneLayout = playerSetting.getPlayerFactory()
      .map(PlayerFactory::getIntegrationMethod)
      .map(im -> im == IntegrationMethod.WINDOW ? SceneLayout.CHILD : SceneLayout.ROOT)
      .orElse(SceneLayout.ROOT);

    sceneManager.setSceneLayout(sceneLayout);
    sceneManager.display();

    RootPresentation rootPresentation = new RootPresentation();

    rootPresentation.childPresentation.set(provider.get());

    sceneManager.getRootPane().getChildren().setAll(ViewPort.fixed(injector.getInstance(Theme.class), rootPresentation, null));

    logDisplayStats(sceneManager);
  }

  private static void logDisplayStats(SceneManager sceneManager) {
    Screen screen = sceneManager.getScreen();
    Rectangle2D bounds = screen.getBounds();

    LOGGER.info("Screen size: " + bounds.getWidth() + "x" + bounds.getHeight() + " [" + bounds.getWidth() * screen.getOutputScaleX() + "x" + bounds.getHeight() * screen.getOutputScaleY() + "]");
    LOGGER.info("Screen dpi: " + screen.getDpi());
    LOGGER.info("Screen OutputScaleX/Y: " + screen.getOutputScaleX() + "x" + screen.getOutputScaleY());

    Window window = sceneManager.getScene().getWindow();

    LOGGER.info("Window size: " + window.getWidth() + "x" + window.getHeight() + " [" + window.getWidth() * window.getOutputScaleX() + "x" + window.getHeight() * window.getOutputScaleY() + "]");
    LOGGER.info("Window OutputScaleX/Y: " + window.getOutputScaleX() + "x" + window.getOutputScaleY());
    LOGGER.info("Window RenderScaleX/Y: " + window.getRenderScaleX() + "x" + window.getRenderScaleY());
  }

  private static void loadPlayerPlugins(ComponentScannerFactory componentScannerFactory, CandidateRegistry registry) throws IOException {
    PluginManager pluginManager = new PluginManager(componentScannerFactory, registry);
    List<Plugin> plugins = new ArrayList<>();
    Path root = Paths.get("ui-plugins");

    if(Files.exists(root)) {
      LOGGER.fine("Loading UI plugins from: " + root);

      Files.find(root, 1, (p, a) -> !p.equals(root)).forEach(p -> {
        try {
          List<URL> urls = Files.find(p, 1, (cp, a) -> !cp.equals(p)).map(Path::toUri).map(uri -> {
            try {
              LOGGER.fine("Found UI plugin: " + uri);

              return uri.toURL();
            }
            catch(MalformedURLException e) {
              throw new IllegalStateException(e);
            }
          }).collect(Collectors.toList());

          if(!urls.isEmpty()) {
            plugins.add(pluginManager.loadPluginAndScan(urls.toArray(new URL[] {})));
          }
        }
        catch(IOException e) {
          throw new IllegalStateException(e);
        }
      });
    }
    else {
      plugins.add(pluginManager.loadPluginAndScan(
        URI.create("file:/P:/Dev/git/MediaSystem-v2/mediasystem-ext-vlc/target/classes/").toURL(),
        new URL("file:P:/Dev/git/MediaSystem-v2/mediasystem-ext-vlc/target/dependencies-only.jar")
      ));
      plugins.add(pluginManager.loadPluginAndScan(
        URI.create("file:/P:/Dev/git/MediaSystem-v2/mediasystem-ext-mpv/target/classes/").toURL(),
        new URL("file:P:/Dev/git/MediaSystem-v2/mediasystem-ext-mpv/target/dependencies-only.jar")
      ));
    }

    LOGGER.info(plugins.size() + " UI plugins loaded from: " + root);
  }
}
