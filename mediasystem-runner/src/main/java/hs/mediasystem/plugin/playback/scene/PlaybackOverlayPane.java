package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.ui.api.player.PlayerPresentation;
import hs.mediasystem.util.image.ImageHandle;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.SpecialEffects;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.GridPaneUtil;
import hs.mediasystem.util.javafx.control.Labels;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.effect.Blend;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Reflection;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class PlaybackOverlayPane extends StackPane {
  private static final String STYLES_URL = LessLoader.compile(PlaybackOverlayPane.class, "styles.less");

  public final ObjectProperty<PlayerPresentation> player = new SimpleObjectProperty<>();
  public final BooleanProperty overlayVisible = new SimpleBooleanProperty(true);

  private final ObjectProperty<PlaybackOverlayPresentation> presentation = new SimpleObjectProperty<>();
  private final PlayerBindings playerBindings = new PlayerBindings(player);

  private final GridPane detailsOverlay = GridPaneUtil.create(new double[] {5, 20, 5, 65, 5}, new double[] {45, 50, 5});

  private final ObservableValue<ImageHandle> posterHandle;
  private final AsyncImageProperty poster = new AsyncImageProperty();

  private final PlaybackInfoBorders borders = new PlaybackInfoBorders(playerBindings);

  private final VBox playbackStateOverlay = new VBox() {{
    getStyleClass().add("content-box");
    setVisible(false);
  }};

  private final HBox conditionalStateOverlay = new HBox() {{
    setSpacing(30);
    setAlignment(Pos.BOTTOM_LEFT);
    setFillHeight(false);
  }};

  private final Timeline fadeInSustainAndFadeOut = new Timeline(
    new KeyFrame(Duration.seconds(0)),
    new KeyFrame(Duration.seconds(1), new KeyValue(detailsOverlay.opacityProperty(), 1.0)),
    new KeyFrame(Duration.seconds(6), new KeyValue(detailsOverlay.opacityProperty(), 1.0)),
    new KeyFrame(Duration.seconds(9), new KeyValue(detailsOverlay.opacityProperty(), 0.0))
  );

  public PlaybackOverlayPane(PlaybackOverlayPresentation presentation) {
    this.presentation.set(presentation);
    this.posterHandle = this.presentation.map(p -> p.work).map(Work::getDetails).map(d -> d.getCover().or(d::getSampleImage).orElse(null));
    this.player.set(presentation.playerPresentation.get());
    this.overlayVisible.bind(presentation.overlayVisible);

    getStylesheets().add(STYLES_URL);

    setId("playback-overlay");

    poster.imageHandleProperty().bind(posterHandle);

    playbackStateOverlay.getChildren().addListener(new ListChangeListener<Node>() {
      @Override
      public void onChanged(ListChangeListener.Change<? extends Node> change) {
        playbackStateOverlay.setVisible(!change.getList().isEmpty());
      }
    });

    overlayVisible.addListener(new ChangeListener<Boolean>() {
      @Override
      public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old, Boolean current) {
        borders.clockAndPositionVisible.setValue(current.booleanValue());
      }
    });

    setFocusTraversable(true);

    detailsOverlay.setId("video-overlay");
    detailsOverlay.add(new BiasedImageView() {{
      GridPane.setValignment(this, VPos.BOTTOM);
      GridPane.setHalignment(this, HPos.RIGHT);
      imageProperty().bind(poster);
      setPreserveRatio(true);
      setAlignment(Pos.BOTTOM_RIGHT);
      setEffect(new Blend() {{
        setBottomInput(new DropShadow());
        setTopInput(new Reflection() {{
          this.setFraction(0.10);
        }});
      }});
    }}, 1, 1);
    detailsOverlay.add(new BorderPane() {{
      setId("video-overlay_info");
      setBottom(new HBox() {{
        getChildren().add(new VBox() {{
          ObservableValue<String> serieName = PlaybackOverlayPane.this.presentation.map(pop -> pop.work).map(Work::getContext).map(o -> o.orElse(null)).map(p -> p.type().isSerie() ? p.title() : null);
          ObservableValue<String> title = PlaybackOverlayPane.this.presentation.map(pop -> pop.work).map(Work::getDetails).map(Details::getTitle);

          HBox.setHgrow(this, Priority.ALWAYS);
          getChildren().addAll(
            Labels.create("video-subtitle", serieName.orElse(""), Labels.HIDE_IF_EMPTY),
            Labels.create("video-title", title, Labels.apply(label -> label.setEffect(SpecialEffects.createNeonEffect(64))))
          );
        }});
      }});
    }}, 3, 1);

    getChildren().add(detailsOverlay);
    getChildren().add(borders);

    getChildren().add(new GridPane() {{
      setHgap(0);
      getColumnConstraints().add(new ColumnConstraints() {{
        setPercentWidth(5);
      }});
      getColumnConstraints().add(new ColumnConstraints() {{
        setPercentWidth(20);
      }});
      getColumnConstraints().add(new ColumnConstraints() {{
        setPercentWidth(50);
      }});
      getColumnConstraints().add(new ColumnConstraints() {{
        setPercentWidth(25);
      }});
      getRowConstraints().add(new RowConstraints() {{
        setPercentHeight(10);
      }});
      getRowConstraints().add(new RowConstraints());
      getRowConstraints().add(new RowConstraints() {{
        setVgrow(Priority.ALWAYS);
        setFillHeight(true);
      }});
      getRowConstraints().add(new RowConstraints() {{
        setPercentHeight(5);
      }});

      add(playbackStateOverlay, 2, 1);
      add(conditionalStateOverlay, 1, 2);
    }});

    sceneProperty().addListener(o -> {
      if(getScene() == null) {
        fadeInSustainAndFadeOut.stop();
      }
      else {
        fadeInSustainAndFadeOut.playFromStart();
      }
    });

    registerConditionalOSD(playerBindings.muted, new BorderPane() {{
      getStyleClass().add("content-box");
      setCenter(new Region() {{
        setMinSize(40, 40);
        getStyleClass().add("mute-shape");
      }});
    }});

    registerConditionalOSD(playerBindings.paused, new VBox() {{
      getStyleClass().add("content-box");
      getChildren().add(new Region() {{
        setMinSize(40, 40);
        getStyleClass().add("pause-shape");
      }});
    }});
  }

  public void showOSD() {
    fadeInSustainAndFadeOut.playFromStart();
  }

  public final void registerConditionalOSD(BooleanExpression showCondition, final Node node) {  // id of node is used to distinguish same items
    node.setOpacity(0);
    conditionalStateOverlay.getChildren().add(node);

    final Timeline fadeIn = new Timeline(
      new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), 0.0)),
      new KeyFrame(Duration.seconds(0.5), new KeyValue(node.opacityProperty(), 1.0))
    );

    final Timeline fadeOut = new Timeline(
      new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), 1.0)),
      new KeyFrame(Duration.seconds(0.5), new KeyValue(node.opacityProperty(), 0.0))
    );

    showCondition.addListener(new ChangeListener<Boolean>() {
      @Override
      public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean value) {
        if(value) {
          fadeIn.play();
        }
        else {
          fadeOut.play();
        }
      }
    });
  }
}
