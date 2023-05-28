package hs.mediasystem.runner.root;

import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.presentation.PresentationActionEvent;
import hs.mediasystem.presentation.PresentationActionFiredEvent;
import hs.mediasystem.runner.action.FXControlFactory;
import hs.mediasystem.runner.action.InputActionHandler;
import hs.mediasystem.runner.presentation.ViewPort;
import hs.mediasystem.runner.presentation.ViewPortFactory;
import hs.mediasystem.runner.root.ParentalControlsProvider.ParentalControls;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.runner.util.SceneManager;
import hs.mediasystem.runner.util.action.ActionTarget;
import hs.mediasystem.util.bg.BackgroundTaskRegistry;
import hs.mediasystem.util.bg.BackgroundTaskRegistry.Workload;
import hs.mediasystem.util.javafx.SceneUtil;
import hs.mediasystem.util.javafx.SpecialEffects;
import hs.mediasystem.util.javafx.base.Events;
import hs.mediasystem.util.javafx.base.FocusEvent;
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

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
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
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class RootNodeFactory implements NodeFactory<RootPresentation> {
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);
  private static final String ROOT_STYLES_URL = LessLoader.compile(RootNodeFactory.class, "root.less");
  private static final String ACTION_DISPLAY_POPUP_STYLES_URL = LessLoader.compile(RootNodeFactory.class, "action-display-popup.less");
  private static final String PROGRESS_STYLES_URL = LessLoader.compile(RootNodeFactory.class, "progress-pane.less");
  private static final String CLOCK_STYLES_URL = LessLoader.compile(RootNodeFactory.class, "clock-pane.less");
  private static final String LOGO_STYLES_URL = LessLoader.compile(RootNodeFactory.class, "logo-pane.less");
  private static final String FPS_STYLES_URL = LessLoader.compile(RootNodeFactory.class, "fps-pane.less");

  @Inject private SceneManager sceneManager;
  @Inject private ViewPortFactory viewPortFactory;
  @Inject private Provider<ParentalControls> parentalControlsProvider;
  @Inject private FXControlFactory controlFactory;
  @Inject private InputActionHandler inputActionHandler;

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

    StackPane clockPane = Containers.stack("clock-pane", vbox);

    clockPane.visibleProperty().bind(presentation.clockVisible);
    clockPane.getStylesheets().add(CLOCK_STYLES_URL);

    StackPane logoPane = Containers.stack("logo-pane", createLogo());

    logoPane.visibleProperty().bind(presentation.clockVisible);
    logoPane.getStylesheets().add(LOGO_STYLES_URL);

    StackPane fpsLayer = Containers.stack("fps-layer", createFrameRateMeter());

    fpsLayer.visibleProperty().bind(presentation.fpsGraphVisible);

    StackPane progressPane = createProgressPane(presentation);

    logoPane.setMouseTransparent(true);
    progressPane.setMouseTransparent(true);
    clockPane.setMouseTransparent(true);

    StackPane stackPane = Containers.stack(viewPort, logoPane, progressPane, clockPane, fpsLayer);
    ParentalControls parentalControls = parentalControlsProvider.get();

    stackPane.getStylesheets().add(ROOT_STYLES_URL);

    if(parentalControls.passcode != null && !parentalControls.passcode.isEmpty()) {
      stackPane.addEventHandler(KeyEvent.KEY_TYPED, new PinCodeEventHandler(presentation, parentalControls.passcode));
    }

    stackPane.addEventHandler(PresentationActionFiredEvent.ANY, new ActionDisplayPopupEventHandler());

    return stackPane;
  }

  class ActionDisplayPopupEventHandler implements EventHandler<PresentationActionFiredEvent> {
    private static final Duration CLOSE_TIME = Duration.seconds(2.0);
    private static final Duration FADE_DURATION = Duration.seconds(0.2);

    private Popup popup;

    class Popup {
      private final ActionTarget target;
      private final Stage stage = new Stage(StageStyle.TRANSPARENT);
      private final HBox hbox = Containers.hbox("root, media-look, action-display-popup");
      private final Scene scene = SceneUtil.createScene(hbox);
      private final Timeline timeline = new Timeline(
        new KeyFrame(FADE_DURATION, new KeyValue(hbox.opacityProperty(), 1.0)),
        new KeyFrame(CLOSE_TIME, new KeyValue(hbox.opacityProperty(), 1.0)),
        new KeyFrame(CLOSE_TIME.add(FADE_DURATION), new KeyValue(hbox.opacityProperty(), 0.0)),
        new KeyFrame(CLOSE_TIME.add(FADE_DURATION).add(FADE_DURATION), e -> {
          if(this == popup) {
            popup = null;  // only clear if it wasn't replaced yet by another
          }

          stage.close();
        })  // allow FADE_DURATION of time in case user pressed BACK when fading out to prevent an unwanted BACK
      );

      Popup(Scene sourceScene, ActionTarget target, Node... nodes) {
        this.target = target;

        inputActionHandler.attachToScene(scene);

        scene.getStylesheets().add(ACTION_DISPLAY_POPUP_STYLES_URL);
        scene.setFill(Color.TRANSPARENT);

        hbox.setOpacity(0);
        hbox.getChildren().setAll(nodes);

        scene.addEventFilter(EventType.ROOT, e -> {
          if(CLOSE_TIME.greaterThan(timeline.getCurrentTime())) {
            timeline.playFromStart();
          }
        });

        scene.addEventHandler(PresentationActionEvent.PROPOSED, e -> {
          if(e.getAction().getPath().equals("navigateBack")) {
            if(CLOSE_TIME.greaterThan(timeline.getCurrentTime())) {
              timeline.playFrom(CLOSE_TIME);
            }

            e.consume();
          }
        });

        scene.addEventHandler(EventType.ROOT, e -> {
          if(e.isConsumed()) {
            return;  // consumed events apparently are in some cases being delivered (when at the same level in the capturing/bubbling hierarchy); not sure if this is a bug or not, but definitely seems odd
          }

          if(e.getEventType().equals(KeyEvent.KEY_RELEASED) || e.getEventType().equals(KeyEvent.KEY_TYPED) || e instanceof ActionEvent || e instanceof FocusEvent) {
            return;
          }

          if(CLOSE_TIME.greaterThan(timeline.getCurrentTime())) {
            timeline.playFrom(CLOSE_TIME);
          }

          if(e instanceof PresentationActionEvent && Events.dispatchEvent(sourceScene.getFocusOwner(), e)) {
            e.consume();
          }
        });

        timeline.playFromStart();

        stage.initOwner(sourceScene.getWindow());
        stage.setScene(scene);
        stage.show();
      }
    }

    @Override
    public void handle(PresentationActionFiredEvent event) {
      ActionTarget actionTarget = event.getActionTarget();

      if(!actionTarget.isVisible()) {
        return;  // prevents showing controls that are unsuited for interaction (pause, player position)
      }

      if(popup != null) {
        if(actionTarget.equals(popup.target)) {
          popup.timeline.playFromStart();
        }
        else {
          if(CLOSE_TIME.greaterThan(popup.timeline.getCurrentTime())) {
            popup.timeline.playFrom(CLOSE_TIME);  // close immediately if it wasn't closing yet
          }

          popup = null;
        }
      }

      if(popup == null) {
        Node control = controlFactory.createInteractiveControl(actionTarget, event.getPresentation());

        if(control != null && event.getTarget() instanceof Node node) {
          popup = new Popup(node.getScene(), actionTarget, Labels.create("title", actionTarget.getLabel()), control);
        }
      }
    }
  }

  private static final int FPS_SIZE = 300;

  private static Pane createFrameRateMeter() {
    Label fpsLabel = new Label();
    Canvas canvas = new Canvas(FPS_SIZE, 100);
    GraphicsContext g2d = canvas.getGraphicsContext2D();

    g2d.setStroke(new Color(1.0, 1.0, 1.0, 0.5));

    Pane fpsPane = Containers.stack("fps-pane", fpsLabel, canvas);

    fpsPane.setMaxWidth(Region.USE_PREF_SIZE);
    fpsPane.setMaxHeight(Region.USE_PREF_SIZE);

    fpsPane.getStylesheets().add(FPS_STYLES_URL);

    AnimationTimer frameRateMeter = new AnimationTimer() {
      private final long[] frameDeltas = new long[FPS_SIZE];

      private int frameTimeIndex = 0;
      private long lastNanos = 0;
      private long then = System.nanoTime();
      private int elementsFilled = 0;

      @Override
      public void handle(long now) {
        frameDeltas[frameTimeIndex] = now - then;
        frameTimeIndex = (frameTimeIndex + 1) % frameDeltas.length;

        if(elementsFilled < frameDeltas.length) {
          elementsFilled++;
        }

        long elapsedNanos = 0;
        double min = Double.MAX_VALUE;
        double max = 0;

        for(int i = 0; i < elementsFilled; i++) {
          min = Math.min(min, frameDeltas[i]);
          max = Math.max(max, frameDeltas[i]);
          elapsedNanos += frameDeltas[i];
        }

        long elapsedNanosPerFrame = elapsedNanos / elementsFilled;
        double frameRate = 1_000_000_000.0 / elapsedNanosPerFrame;

        g2d.clearRect(frameTimeIndex, 0, 1, 100);
        g2d.strokeLine(frameTimeIndex, 100, frameTimeIndex, 100 - (now - then) / 1000000.0);

        if(now - lastNanos > 100 * 1000 * 1000) {
          fpsLabel.setText(String.format(" %.1f/%.1f ms - %.0f fps", min / 1000000.0, max / 1000000.0, frameRate));
          lastNanos = now;
        }

        then = now;
      }
    };

    frameRateMeter.start();

    return fpsPane;
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
