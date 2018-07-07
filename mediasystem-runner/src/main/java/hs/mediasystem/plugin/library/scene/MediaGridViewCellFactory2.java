package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.javafx.AsyncImageProperty2;
import hs.mediasystem.util.javafx.Labels;
import hs.mediasystem.util.javafx.ScaledImageView;

import java.util.function.Function;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
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

public class MediaGridViewCellFactory2<T> implements Callback<ListView<T>, ListCell<T>> {
  public enum MediaStatus {
    UNAVAILABLE,
    AVAILABLE,
    WATCHED
  }

  private Function<T, String> titleExtractor;
  private Function<T, ImageHandle> imageHandleExtractor;
  private Function<T, String> detailExtractor;
  private Function<T, MediaStatus> mediaStatusExtractor;
  private Function<T, String> sequenceNumberExtractor;

  public void setTitleExtractor(Function<T, String> extractor) {
    this.titleExtractor = extractor;
  }

  public void setImageExtractor(Function<T, ImageHandle> extractor) {
    this.imageHandleExtractor = extractor;
  }

  public void setDetailExtractor(Function<T, String> extractor) {
    this.detailExtractor = extractor;
  }

  public void setMediaStatusExtractor(Function<T, MediaStatus> extractor) {
    this.mediaStatusExtractor = extractor;
  }

  public void setSequenceNumberExtractor(Function<T, String> extractor) {
    this.sequenceNumberExtractor = extractor;
  }

  @Override
  public ListCell<T> call(ListView<T> param) {
    return new ListCell<T>() {
      private final ScaledImageView imageView = new ScaledImageView(Labels.create("?", "media-grid-view-image-place-holder"));
      private final AsyncImageProperty2 asyncImageProperty = new AsyncImageProperty2();
      private final Label name = Labels.create("name");
      private final Label detail = Labels.create("detail");
      private final Label indicator = Labels.create("indicator");
      private final Label indicatorBackground = Labels.create("indicator-background");
      private final StackPane indicatorPane = new StackPane(indicatorBackground, indicator);
      private final Label sequenceNumber = Labels.create("sequence-number");
      private final StackPane sequenceNumberPane = new StackPane(sequenceNumber);
//      private final WeakBinder binder = new WeakBinder();

      private final VBox vbox = new VBox() {{
        getChildren().add(imageView);
        getChildren().add(name);
        getChildren().add(detail);

        setAlignment(Pos.CENTER);
        VBox.setVgrow(imageView, Priority.ALWAYS);
      }};

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
      protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

//        binder.unbindAll();
        asyncImageProperty.removeListener(imageChangeListener);

        if(!empty) {
          setGraphic(vbox);

//          binder.bind(name.textProperty(), titleExtractor.apply(item));
//          binder.bind(asyncImageProperty.imageHandleProperty(), imageHandleExtractor.apply(item));
          name.setText(titleExtractor.apply(item));
          asyncImageProperty.imageHandleProperty().set(imageHandleExtractor.apply(item));

          if(asyncImageProperty.get() == null) {
            asyncImageProperty.addListener(imageChangeListener);
          }

          if(detailExtractor != null) {
//            binder.bind(detail.textProperty(), detailExtractor.apply(item));
            detail.setText(detailExtractor.apply(item));
          }

          if(mediaStatusExtractor != null) {
            switch(mediaStatusExtractor.apply(item)) {
            case WATCHED:
              getStyleClass().add("watched");
              getStyleClass().remove("available");
              break;
            case AVAILABLE:
              getStyleClass().add("available");
              getStyleClass().remove("watched");
              break;
            }
          }

          if(sequenceNumberExtractor != null) {
            sequenceNumber.setText(sequenceNumberExtractor.apply(item));
          }
        }
        else {
          setGraphic(null);
          asyncImageProperty.imageHandleProperty().set(null);  // Helps to cancel bg loading of images when cells quickly change
          getStyleClass().remove("watched");
          getStyleClass().remove("available");
        }
      }
    };
  }
}
