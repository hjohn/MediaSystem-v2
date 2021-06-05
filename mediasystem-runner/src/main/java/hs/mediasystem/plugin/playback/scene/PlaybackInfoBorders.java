package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.util.javafx.control.GridPaneUtil;
import hs.mediasystem.util.javafx.control.RangeBar;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.reactfx.value.Val;
import org.reactfx.value.Var;

public class PlaybackInfoBorders extends StackPane {
  public final Var<Boolean> clockAndPositionVisible = Var.newSimpleVar(true);

  private final StringProperty formattedTime = new SimpleStringProperty();

  private final StackPane leftOSDPane = new StackPane();
  private final StackPane centerOSDPane = new StackPane();
  private final StackPane rightOSDPane = new StackPane();

  private final Label positionLabel;
  private final Label timeLabel;

  public PlaybackInfoBorders(PlayerBindings playerBindings) {
    getStyleClass().add("grid");

    positionLabel = new Label() {{
      getStyleClass().add("position");
      GridPane.setHalignment(this, HPos.LEFT);
      GridPane.setValignment(this, VPos.BOTTOM);
      visibleProperty().bind(Val.combine(playerBindings.length.map(l -> l != 0), clockAndPositionVisible, (a, b) -> a && b));
      textProperty().bind(Bindings.concat(playerBindings.formattedPosition, " / ", playerBindings.formattedLength));
    }};

    timeLabel = new Label() {{
      getStyleClass().add("time");
      GridPane.setHalignment(this, HPos.RIGHT);
      GridPane.setValignment(this, VPos.BOTTOM);
      visibleProperty().bind(clockAndPositionVisible);
      textProperty().bind(formattedTime);
    }};

    final GridPane grid = GridPaneUtil.create(new double[] {0.5, 3, 26, 7.5, 26, 7.5, 26, 3, 0.5}, new double[] {50, 45, 4.5, 0.5});

    // Prevent GridPane from becoming bigger than its container when children donot fit in the allocated grid slots:
    grid.setMinSize(1, 1);
    grid.setPrefSize(1, 1);

    grid.add(positionLabel, 1, 2, 2, 1);
    grid.add(timeLabel, 6, 2, 2, 1);

    grid.add(leftOSDPane, 2, 1);
    grid.add(centerOSDPane, 4, 1);
    grid.add(rightOSDPane, 6, 1);

    Timeline updater = new Timeline(
      new KeyFrame(Duration.seconds(1), event -> formattedTime.set(String.format("%1$tR", System.currentTimeMillis())))
    );

    updater.setCycleCount(Animation.INDEFINITE);

    sceneProperty().addListener(o -> {
      if(getScene() == null) {
        updater.stop();
      }
      else {
        updater.play();
      }
    });

    getChildren().add(grid);

    leftOSDPane.setUserData(createOSDTimeLine(leftOSDPane));
    centerOSDPane.setUserData(createOSDTimeLine(centerOSDPane));
    rightOSDPane.setUserData(createOSDTimeLine(rightOSDPane));

    final Node volume = createOSDItem("Volume", playerBindings.volume, playerBindings.formattedVolume, 0.0, 100.0, 0.0);
    final Node position = createOSDItem("Position", playerBindings.position.map(v -> playerBindings.length.getValue() == 0 ? 0 : v * 100 / playerBindings.length.getValue()), playerBindings.formattedPosition, 0.0, 100.0, 0.0);
    final Node rate = createOSDItem("Playback Speed", playerBindings.rate, playerBindings.formattedRate, 0.0, 4.0, 0.0);
    final Node audioDelay = createOSDItem("Audio Delay", playerBindings.audioDelay.map(v -> v / 1000.0), playerBindings.formattedAudioDelay, -120.0, 120.0, 0.0);
    final Node subtitleDelay = createOSDItem("Subtitle Delay", playerBindings.subtitleDelay.map(v -> v / 1000.0), playerBindings.formattedSubtitleDelay, -120.0, 120.0, 0.0);
    final Node brightness = createOSDItem("Brightness Adjustment", playerBindings.brightness.map(v -> (v - 1.0) * 100), playerBindings.formattedBrightness, -100.0, 100.0, 0.0);
    final Node audioTrack = createOSDItem("Audio Track", playerBindings.formattedAudioTrack);
    final Node subtitle = createOSDItem("Subtitle", playerBindings.formattedSubtitle);

    leftOSDPane.getChildren().add(volume);
    centerOSDPane.getChildren().addAll(rate, audioDelay, subtitleDelay, brightness, audioTrack, subtitle);
    rightOSDPane.getChildren().add(position);

    show(leftOSDPane, null);
    show(centerOSDPane, null);
    show(rightOSDPane, null);

    playerBindings.position.addListener((observable, oldValue, newValue) -> {
      if(Math.abs(oldValue.longValue() - newValue.longValue()) > 2500) {
        showOSD(position);
      }
    });

    playerBindings.volume.addListener((observable, oldValue, newValue) -> showOSD(volume));
    playerBindings.rate.addListener((observable, oldValue, newValue) -> showOSD(rate));
    playerBindings.audioDelay.addListener((observable, oldValue, newValue) -> showOSD(audioDelay));
    playerBindings.subtitleDelay.addListener((observable, oldValue, newValue) -> showOSD(subtitleDelay));
    playerBindings.brightness.addListener((observable, oldValue, newValue) -> showOSD(brightness));
    playerBindings.audioTrack.addListener(new FirstChangeFilter<>((observable, oldValue, value) -> showOSD(audioTrack)));
    playerBindings.subtitle.addListener(new FirstChangeFilter<>((observable, oldValue, value) -> showOSD(subtitle)));
  }

  private static Timeline createOSDTimeLine(Node node) {
    return new Timeline(
      new KeyFrame(Duration.seconds(0.5), new KeyValue(node.opacityProperty(), 1.0)),
      new KeyFrame(Duration.seconds(2), new KeyValue(node.opacityProperty(), 1.0)),
      new KeyFrame(Duration.seconds(4), new KeyValue(node.opacityProperty(), 0.0))
    );
  }

  private static void showOSD(Node node) {
    Parent parent = node.getParent();

    ((Timeline)parent.getUserData()).playFromStart();

    show(parent, node);
  }

  private static Node createOSDItem(final String title, final ObservableValue<? extends Number> value, final ObservableValue<String> valueText, final double min, final double max, final double origin) {
    return new HBox() {{
      setFillHeight(false);
      setAlignment(Pos.BOTTOM_CENTER);
      getChildren().add(new VBox() {{
        HBox.setHgrow(this, Priority.ALWAYS);
        getStyleClass().add("osd");

        GridPane gridPane = GridPaneUtil.create(new double[] {30, 5, 65}, new double[] {100});

        getChildren().add(gridPane);

        Label titleLabel = new Label(title);
        GridPane.setValignment(titleLabel, VPos.TOP);

        gridPane.add(titleLabel, 0, 0);
        gridPane.add(new Label() {{
          textProperty().bind(valueText);
          setWrapText(true);
        }}, 2, 0);

        if(value != null) {
          RangeBar bar = new RangeBar(min, max, origin);

          bar.valueProperty().bind(value);

          getChildren().add(bar);
        }
      }});
    }};
  }

  private static Node createOSDItem(final String title, final ObservableValue<String> valueText) {
    return createOSDItem(title, null, valueText, 0.0, 0.0, 0.0);
  }

  private static void show(Parent pane, Node node) {
    for(Node child : pane.getChildrenUnmodifiable()) {
      boolean show = child.equals(node);

      child.setManaged(show);
      child.setVisible(show);
    }
  }

  public static class FirstChangeFilter<T> implements ChangeListener<T> {
    private final ChangeListener<T> changeListener;

    private boolean notFirst;

    public FirstChangeFilter(ChangeListener<T> changeListener) {
      this.changeListener = changeListener;
    }

    @Override
    public void changed(ObservableValue<? extends T> observable, T oldValue, T value) {
      if(notFirst) {
        changeListener.changed(observable, oldValue, value);
      }

      notFirst = true;
    }
  }
}
