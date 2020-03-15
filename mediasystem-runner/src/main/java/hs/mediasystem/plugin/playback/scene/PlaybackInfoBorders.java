package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.ui.api.player.AudioTrack;
import hs.mediasystem.ui.api.player.Subtitle;
import hs.mediasystem.util.javafx.control.GridPaneUtil;
import hs.mediasystem.util.javafx.control.RangeBar;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
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
  private final ObjectProperty<Work> work = new SimpleObjectProperty<>();
  public ObjectProperty<Work> workProperty() { return work; }

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
      new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          formattedTime.set(String.format("%1$tR", System.currentTimeMillis()));
        }
      })
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

    final OSD volume = new OSD("Volume", playerBindings.volume, playerBindings.formattedVolume, 0.0, 100.0, 0.0, leftOSDPane);
    final OSD position = new OSD("Position", playerBindings.position.map(v -> playerBindings.length.getValue() == 0 ? 0 : v * 100 / playerBindings.length.getValue()), playerBindings.formattedPosition, 0.0, 100.0, 0.0, rightOSDPane);
    final OSD rate = new OSD("Playback Speed", playerBindings.rate, playerBindings.formattedRate, 0.0, 4.0, 0.0, centerOSDPane);
    final OSD audioDelay = new OSD("Audio Delay", playerBindings.audioDelay.map(v -> v / 1000.0), playerBindings.formattedAudioDelay, -120.0, 120.0, 0.0, centerOSDPane);
    final OSD subtitleDelay = new OSD("Subtitle Delay", playerBindings.subtitleDelay.map(v -> v / 1000.0), playerBindings.formattedSubtitleDelay, -120.0, 120.0, 0.0, centerOSDPane);
    final OSD brightness = new OSD("Brightness Adjustment", playerBindings.brightness.map(v -> (v - 1.0) * 100), playerBindings.formattedBrightness, -100.0, 100.0, 0.0, centerOSDPane);
    final OSD audioTrack = new OSD("Audio Track", playerBindings.formattedAudioTrack, centerOSDPane);
    final OSD subtitle = new OSD("Subtitle", playerBindings.formattedSubtitle, centerOSDPane);

    playerBindings.position.addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        if(Math.abs(oldValue.longValue() - newValue.longValue()) > 2500) {
          showOSD(position);
        }
      }
    });

    playerBindings.volume.addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        showOSD(volume);
      }
    });

    playerBindings.rate.addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        showOSD(rate);
      }
    });

    playerBindings.audioDelay.addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        showOSD(audioDelay);
      }
    });

    playerBindings.subtitleDelay.addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        showOSD(subtitleDelay);
      }
    });

    playerBindings.brightness.addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        showOSD(brightness);
      }
    });

    playerBindings.audioTrack.addListener(new FirstChangeFilter<>(new ChangeListener<AudioTrack>() {
      @Override
      public void changed(ObservableValue<? extends AudioTrack> observable, AudioTrack oldValue, AudioTrack value) {
        showOSD(audioTrack);
      }
    }));

    playerBindings.subtitle.addListener(new FirstChangeFilter<>(new ChangeListener<Subtitle>() {
      @Override
      public void changed(ObservableValue<? extends Subtitle> observable, Subtitle oldValue, Subtitle value) {
        showOSD(subtitle);
      }
    }));
  }

  private static Timeline createOSDTimeLine(Node node) {
    return new Timeline(
      new KeyFrame(Duration.seconds(0.5), new KeyValue(node.opacityProperty(), 1.0)),
      new KeyFrame(Duration.seconds(2), new KeyValue(node.opacityProperty(), 1.0)),
      new KeyFrame(Duration.seconds(4), new KeyValue(node.opacityProperty(), 0.0))
    );
  }

  private static void showOSD(OSD osd) {
    ((Timeline)osd.location.getUserData()).playFromStart();

    if(osd.location.getChildren().isEmpty() || !osd.location.getChildren().get(0).getId().equals(osd.title)) {
      osd.location.getChildren().setAll(osd.node);
    }
  }

  private static class OSD {
    private final String title;
    private final Node node;
    private final StackPane location;

    public OSD(String title, ObservableValue<? extends Number> observableValue, StringExpression valueText, double min, double max, double origin, StackPane location) {
      this.title = title;
      this.location = location;
      this.node = createOSDItem(title, min, max, origin, observableValue, valueText);
    }

    public OSD(String title, StringBinding valueText, StackPane location) {
      this.title = title;
      this.location = location;
      this.node = createOSDItem(title, 0.0, 0.0, 0.0, null, valueText);
    }
  }

  private static Node createOSDItem(final String title, final double min, final double max, final double origin, final ObservableValue<? extends Number> value, final StringExpression valueText) {
    return new HBox() {{
      setId(title);
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
