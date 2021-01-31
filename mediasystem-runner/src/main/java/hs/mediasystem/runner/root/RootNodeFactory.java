package hs.mediasystem.runner.root;

import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.presentation.ViewPort;
import hs.mediasystem.presentation.ViewPortFactory;
import hs.mediasystem.runner.root.ParentalControlsProvider.ParentalControls;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.runner.util.SceneManager;
import hs.mediasystem.util.bg.BackgroundTaskRegistry;
import hs.mediasystem.util.bg.BackgroundTaskRegistry.Workload;
import hs.mediasystem.util.javafx.SpecialEffects;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.GridPane;
import hs.mediasystem.util.javafx.control.GridPaneUtil;
import hs.mediasystem.util.javafx.control.Labels;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class RootNodeFactory implements NodeFactory<RootPresentation> {
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);
  private static final String PROGRESS_STYLES_URL = LessLoader.compile(RootNodeFactory.class, "progress-pane.less");
  private static final String CLOCK_STYLES_URL = LessLoader.compile(RootNodeFactory.class, "clock-pane.less");
  private static final String LOGO_STYLES_URL = LessLoader.compile(RootNodeFactory.class, "logo-pane.less");

  @Inject private SceneManager sceneManager;
  @Inject private ViewPortFactory viewPortFactory;
  @Inject private Provider<ParentalControls> parentalControlsProvider;

  @Override
  public Node create(RootPresentation presentation) {
    Node viewPort = createViewPort(presentation);

    StringProperty time = new SimpleStringProperty();
    StringProperty date = new SimpleStringProperty();
    VBox vbox = Containers.vbox("clock-box", Labels.create("clock", time), Labels.create("date", date));

    Timeline timeline = new Timeline(5,
      new KeyFrame(Duration.seconds(0), e -> {
        ZonedDateTime now = ZonedDateTime.now();

        time.set(now.format(TIME_FORMATTER));
        date.set(now.format(DATE_FORMATTER));
      }),
      new KeyFrame(Duration.seconds(0.1))
    );

    timeline.setCycleCount(Timeline.INDEFINITE);
    timeline.play();

    vbox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

    StackPane clockPane = new StackPane(vbox);

    clockPane.visibleProperty().bind(presentation.clockVisible);
    clockPane.getStyleClass().add("clock-pane");
    clockPane.getStylesheets().add(CLOCK_STYLES_URL);

    StackPane logoPane = new StackPane(createLogo());

    logoPane.visibleProperty().bind(presentation.clockVisible);
    logoPane.getStyleClass().add("logo-pane");
    logoPane.getStylesheets().add(LOGO_STYLES_URL);

/*
    Label fpsLabel = new Label();

    StackPane fpsPane = new StackPane(fpsLabel);

    fpsPane.getStylesheets().add(LessLoader.compile(getClass().getResource("fps-pane.less")).toExternalForm());
    fpsPane.getStyleClass().add("fps-pane");

    AnimationTimer frameRateMeter = new AnimationTimer() {
      private final long[] frameTimes = new long[100];
      private int frameTimeIndex = 0;
      private boolean arrayFilled = false;

      @Override
      public void handle(long now) {
        long oldFrameTime = frameTimes[frameTimeIndex];

        frameTimes[frameTimeIndex] = now;
        frameTimeIndex = (frameTimeIndex + 1) % frameTimes.length;

        if(frameTimeIndex == 0) {
          arrayFilled = true;
        }

        if(arrayFilled) {
          long elapsedNanos = now - oldFrameTime;
          long elapsedNanosPerFrame = elapsedNanos / frameTimes.length;
          double frameRate = 1_000_000_000.0 / elapsedNanosPerFrame;
          double min = Double.MAX_VALUE;
          double max = 0;

          for(int i = 0; i < frameTimes.length - 1; i++) {
            long d = frameTimes[(i + frameTimeIndex + 1) % frameTimes.length] - frameTimes[(i + frameTimeIndex) % frameTimes.length];

            min = Math.min(min, d);
            max = Math.max(max, d);
          }

          fpsLabel.setText(String.format(" %.1f/%.1f ms - %.1f ", min / 1000000.0, max / 1000000.0, frameRate));
        }
      }
    };

    frameRateMeter.start();
*/
    StackPane progressPane = createProgressPane(presentation);

    logoPane.setMouseTransparent(true);
    progressPane.setMouseTransparent(true);
    clockPane.setMouseTransparent(true);

    StackPane stackPane = new StackPane(viewPort, logoPane, progressPane, clockPane); //, fpsPane);
    ParentalControls parentalControls = parentalControlsProvider.get();

    if(parentalControls.passcode != null && !parentalControls.passcode.isEmpty()) {
      stackPane.addEventHandler(KeyEvent.KEY_TYPED, new PinCodeEventHandler(presentation, parentalControls.passcode));
    }

    return stackPane;
  }

  private static class PinCodeEventHandler implements EventHandler<KeyEvent> {
    private static final Logger LOGGER = Logger.getLogger(PinCodeEventHandler.class.getName());

    private final RootPresentation presentation;
    private final String pinCode;

    private String lastCharsTyped;
    private long lastEventMillis;

    public PinCodeEventHandler(RootPresentation presentation, String pinCode) {
      this.presentation = presentation;
      this.pinCode = pinCode;
    }

    @Override
    public void handle(KeyEvent e) {
      long eventMillis = System.currentTimeMillis();

      if(eventMillis > lastEventMillis + 5000) {  // discard buffer if last event was more than 5 seconds ago
        lastCharsTyped = "";
      }

      lastCharsTyped += e.getCharacter();

      if(lastCharsTyped.endsWith(pinCode)) {
        boolean value = !presentation.hiddenItemsVisible.getValue();

        presentation.hiddenItemsVisible.setValue(value);
        lastCharsTyped = "";

        LOGGER.info("Toggled visibility of hidden items to: " + value);
      }

      lastEventMillis = eventMillis;
    }
  }

  public StackPane createProgressPane(RootPresentation presentation) {
    GridPane gp = GridPaneUtil.create(new double[] {20, 20, 25, 15, 20}, new double[] {100});

    VBox vbox = new VBox();

    vbox.getStyleClass().add("vbox");

    gp.add(vbox, 3, 0);

    StackPane pane = new StackPane(gp);
    BooleanProperty busy = new SimpleBooleanProperty();

    Timeline timeline = new Timeline(5,
      new KeyFrame(Duration.seconds(0), new EventHandler<ActionEvent>() {
        private final Map<Workload, WorkloadProperties> knownWorkloads = new HashMap<>();

        class WorkloadProperties {
          final Workload workload;
          final long SETTLE_TIME = 5000;

          StringProperty progressTextProperty = new SimpleStringProperty();
          DoubleProperty progressProperty = new SimpleDoubleProperty();
          ProgressBar progressBar = new ProgressBar();
          Label label = Labels.create("progress-text", progressTextProperty);
          long firstSeen = System.currentTimeMillis();
          long lastSeen = System.currentTimeMillis();
          boolean displayed;

          WorkloadProperties(Workload workload) {
            this.workload = workload;
            progressBar.setMaxWidth(Double.MAX_VALUE);
            progressBar.progressProperty().bind(progressProperty);
          }

          void update() {
            progressTextProperty.set(workload.getDescription() + " (" + workload.getCompleted() + " of " + workload.getTotal() + ")");
            progressProperty.set(workload.getCompleted() / (double)workload.getTotal());

            if(progressProperty.get() < 1.0) {
              lastSeen = System.currentTimeMillis();
            }
          }

          void display() {
            vbox.getChildren().addAll(progressBar, label);
            displayed = true;
          }

          void dispose() {
            if(displayed) {
              vbox.getChildren().removeAll(progressBar, label);
              displayed = false;
            }

            knownWorkloads.remove(workload);
          }

          boolean shouldDisplay() {
            return System.currentTimeMillis() - firstSeen > SETTLE_TIME && isActive() && !displayed;
          }

          boolean isNotActive() {
            return !isActive();
          }

          boolean isActive() {
            return (displayed && System.currentTimeMillis() - lastSeen < SETTLE_TIME) || progressProperty.get() < 1.0;
          }
        }

        @Override
        public void handle(ActionEvent event) {
          List<Workload> activeWorkloads = BackgroundTaskRegistry.getActiveWorkloads();

          // Add any new workloads:
          activeWorkloads.stream()
            .filter(w -> !knownWorkloads.containsKey(w))
            .forEach(w -> knownWorkloads.put(w, new WorkloadProperties(w)));

          // Create UI for workloads that have been active for SETTLE_TIME seconds:
          knownWorkloads.values().stream()
            .filter(WorkloadProperties::shouldDisplay)
            .forEach(WorkloadProperties::display);

          // Delete workloads that are inactive:
          knownWorkloads.values().stream()
            .filter(WorkloadProperties::isNotActive)
            .collect(Collectors.toList()).stream()
            .forEach(WorkloadProperties::dispose);

          // Update workloads:
          knownWorkloads.values().stream()
            .forEach(WorkloadProperties::update);

          busy.set(!vbox.getChildren().isEmpty());
        }
      }),
      new KeyFrame(Duration.seconds(0.2))
    );

    timeline.setCycleCount(Timeline.INDEFINITE);
    timeline.play();

    pane.visibleProperty().bind(presentation.clockVisible.and(busy));
    pane.getStyleClass().add("progress-pane");
    pane.getStylesheets().add(PROGRESS_STYLES_URL);

    return pane;
  }

  private static Node createLogo() {
    return new HBox() {{
      getStyleClass().addAll("program-name", "element");
      getChildren().add(new Label("Media") {{
        getStyleClass().add("left");
      }});
      getChildren().add(new Label("S") {{
        getStyleClass().add("center");
      }});
      getChildren().add(new Label("ystem") {{
        getStyleClass().add("right");
      }});
      setEffect(SpecialEffects.createNeonEffect(12));
      setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }};
  }

  private final Transition transition = new Transition() {
    {
      setCycleDuration(Duration.millis(500));
    }

    @Override
    protected void interpolate(double frac) {
      sceneManager.getRootPane().setBackground(new Background(new BackgroundFill(new Color(0, 0, 0, frac), null, null)));
    }
  };

  private Node createViewPort(RootPresentation presentation) {
    ViewPort viewPort = viewPortFactory.create(presentation, n -> {

      /*
       * Special functionality for a background node; note that it is possible
       * to have a null background node when native tricks are used to display
       * the background; in those cases the overlayed scene still needs to be
       * made transparent and the clock hidden.
       */

      if(n.getProperties().containsKey("background")) {
        Object backgroundNode = n.getProperties().get("background");

        if(backgroundNode != null) {
          sceneManager.setPlayerRoot(backgroundNode);
        }

        transition.setRate(-1.0);
        transition.playFrom(Duration.millis(500));

        presentation.clockVisible.set(false);
      }
      else {
        presentation.clockVisible.set(true);

        sceneManager.disposePlayerRoot();

        transition.setRate(1.0);
        transition.playFromStart();
      }
    });

    return viewPort;
  }
}
