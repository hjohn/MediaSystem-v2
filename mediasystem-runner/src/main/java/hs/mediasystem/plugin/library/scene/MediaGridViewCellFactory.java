package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.javafx.AsyncImageProperty2;
import hs.mediasystem.util.javafx.Labels;
import hs.mediasystem.util.javafx.ScaledImageView;

import java.util.function.Function;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.Duration;

public class MediaGridViewCellFactory implements Callback<ListView<MediaItem<?>>, ListCell<MediaItem<?>>> {
  public enum MediaStatus {
    UNAVAILABLE,
    AVAILABLE,
    WATCHED
  }

  private Function<MediaItem<?>, ObservableValue<? extends String>> titleBindProvider;
  private Function<MediaItem<?>, ImageHandle> imageHandleExtractor;
  private Function<MediaItem<?>, String> detailExtractor;
  private Function<MediaItem<?>, ObservableValue<? extends MediaStatus>> mediaStatusBindProvider;
  private Function<MediaItem<?>, String> sequenceNumberExtractor;

  public void setTitleBindProvider(Function<MediaItem<?>, ObservableValue<? extends String>> bindProvider) {
    this.titleBindProvider = bindProvider;
  }

  public void setImageExtractor(Function<MediaItem<?>, ImageHandle> extractor) {
    this.imageHandleExtractor = extractor;
  }

  public void setDetailExtractor(Function<MediaItem<?>, String> extractor) {
    this.detailExtractor = extractor;
  }

  public void setMediaStatusBindProvider(Function<MediaItem<?>, ObservableValue<? extends MediaStatus>> bindProvider) {
    this.mediaStatusBindProvider = bindProvider;
  }

  public void setSequenceNumberExtractor(Function<MediaItem<?>, String> extractor) {
    this.sequenceNumberExtractor = extractor;
  }

  @Override
  public ListCell<MediaItem<?>> call(ListView<MediaItem<?>> param) {
    return new ListCell<>() {
      private final ScaledImageView imageView = new ScaledImageView(Labels.create("?", "media-grid-view-image-place-holder"));
      private final AsyncImageProperty2 asyncImageProperty = new AsyncImageProperty2();
      private final Label name = Labels.create("name");
      private final Label detail = Labels.create("detail");
      private final Label indicator = Labels.create("indicator");
      private final Label indicatorBackground = Labels.create("indicator-background");
      private final StackPane indicatorPane = new StackPane(indicatorBackground, indicator);
      private final Label sequenceNumber = Labels.create("sequence-number");
      private final StackPane sequenceNumberPane = new StackPane(sequenceNumber);
      private final ObjectProperty<MediaStatus> mediaStatusProperty = new SimpleObjectProperty<>();
//      private final WeakBinder binder = new WeakBinder();

      private final VBox vbox = new VBox() {{
        getChildren().add(imageView);
        getChildren().add(name);
        getChildren().add(detail);

        setAlignment(Pos.CENTER);
        VBox.setVgrow(imageView, Priority.ALWAYS);
      }};

      private final ChangeListener<? super MediaStatus> updateIndicatorListener = (obs, old, current) -> {
        switch(current == null ? MediaStatus.UNAVAILABLE : current) {
        case WATCHED:
          getStyleClass().add("watched");
          getStyleClass().remove("available");
          break;
        case AVAILABLE:
          getStyleClass().add("available");
          getStyleClass().remove("watched");
          break;
        case UNAVAILABLE:
          getStyleClass().remove("available");
          getStyleClass().remove("watched");
          break;
        }
      };

      {
        imageView.setPreserveRatio(true);
        imageView.imageProperty().bind(asyncImageProperty);
        imageView.setAlignment(Pos.BOTTOM_CENTER);
        imageView.getOverlayRegion().getChildren().add(indicatorPane);
        imageView.getOverlayRegion().getChildren().add(sequenceNumberPane);

        sequenceNumberPane.setAlignment(Pos.BOTTOM_LEFT);
        sequenceNumberPane.getStyleClass().add("sequence-number-pane");
        indicatorPane.setAlignment(Pos.BOTTOM_RIGHT);
        indicatorPane.getStyleClass().add("indicator-pane");

        detail.managedProperty().bind(detail.textProperty().isNotEqualTo(""));
        sequenceNumber.managedProperty().bind(sequenceNumber.textProperty().isNotEqualTo(""));

        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);  // Indicate to cell that it can become as big as possible

        focusedProperty().addListener((obs, old, current) -> {
          new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(scaleXProperty(), getScaleX()), new KeyValue(scaleYProperty(), getScaleY())),
            new KeyFrame(Duration.seconds(0.5), new KeyValue(scaleXProperty(), current ? 1.1 : 1.0), new KeyValue(scaleYProperty(), current ? 1.1 : 1.0))
          ).play();
        });

        this.setCache(true);
        this.setCacheHint(CacheHint.SCALE);
        this.mediaStatusProperty.addListener(updateIndicatorListener);
      }

      private final ChangeListener<? super Image> imageChangeListener = (obs, old, current) -> {
        if(current != null) {
          imageView.setOpacity(0);

          new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(imageView.opacityProperty(), 0)),
            new KeyFrame(Duration.seconds(1), new KeyValue(imageView.opacityProperty(), 1))
          ).play();
        }
      };

      @Override
      protected void updateItem(MediaItem<?> item, boolean empty) {
        super.updateItem(item, empty);

//        binder.unbindAll();
        asyncImageProperty.removeListener(imageChangeListener);

        if(!empty) {
          setGraphic(vbox);

//          binder.bind(name.textProperty(), titleExtractor.apply(item));
//          binder.bind(asyncImageProperty.imageHandleProperty(), imageHandleExtractor.apply(item));
          name.textProperty().bind(titleBindProvider.apply(item));
          asyncImageProperty.imageHandleProperty().set(imageHandleExtractor.apply(item));

          if(asyncImageProperty.get() == null) {
            asyncImageProperty.addListener(imageChangeListener);
          }

          if(detailExtractor != null) {
//            binder.bind(detail.textProperty(), detailExtractor.apply(item));
            detail.setText(detailExtractor.apply(item));
          }

          if(mediaStatusBindProvider != null) {
            mediaStatusProperty.bind(mediaStatusBindProvider.apply(item));
          }

          if(sequenceNumberExtractor != null) {
            sequenceNumber.setText(sequenceNumberExtractor.apply(item));
          }
        }
        else {
          setGraphic(null);

          name.textProperty().unbind();
          mediaStatusProperty.unbind();
          asyncImageProperty.imageHandleProperty().set(null);  // Helps to cancel bg loading of images when cells quickly change
          getStyleClass().remove("watched");
          getStyleClass().remove("available");
        }
      }
    };
  }
}
