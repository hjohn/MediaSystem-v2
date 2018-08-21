package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.plugin.library.scene.MediaItem.MediaStatus;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.javafx.AsyncImageProperty2;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Labels;

import java.util.function.Function;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Duration;

public class MediaGridViewCellFactory<T> implements Callback<ListView<MediaItem<T>>, ListCell<MediaItem<T>>> {
  private Function<MediaItem<T>, ObservableValue<? extends String>> titleBindProvider;
  private Function<MediaItem<T>, ImageHandle> imageHandleExtractor;
  private Function<MediaItem<T>, String> detailExtractor;
  private Function<MediaItem<T>, ObservableValue<? extends MediaStatus>> mediaStatusBindProvider;
  private Function<MediaItem<T>, String> sequenceNumberExtractor;

  private Orientation orientation = Orientation.HORIZONTAL;
  private double aspectRatio = 1.5;

  public void setTitleBindProvider(Function<MediaItem<T>, ObservableValue<? extends String>> bindProvider) {
    this.titleBindProvider = bindProvider;
  }

  public void setImageExtractor(Function<MediaItem<T>, ImageHandle> extractor) {
    this.imageHandleExtractor = extractor;
  }

  public void setDetailExtractor(Function<MediaItem<T>, String> extractor) {
    this.detailExtractor = extractor;
  }

  public void setMediaStatusBindProvider(Function<MediaItem<T>, ObservableValue<? extends MediaStatus>> bindProvider) {
    this.mediaStatusBindProvider = bindProvider;
  }

  public void setSequenceNumberExtractor(Function<MediaItem<T>, String> extractor) {
    this.sequenceNumberExtractor = extractor;
  }

  public void setContentBias(Orientation orientation) {
    this.orientation = orientation;
  }

  public void setPlaceHolderAspectRatio(double aspectRatio) {
    this.aspectRatio = aspectRatio;
  }

  private double maxRatio = Double.MAX_VALUE;
  private double minRatio = 0;

  public void setMinRatio(double minRatio) {
    this.minRatio = minRatio;
  }

  public void setMaxRatio(double maxRatio) {
    this.maxRatio = maxRatio;
  }

  @Override
  public ListCell<MediaItem<T>> call(ListView<MediaItem<T>> param) {
    return new ListCell<>() {
      private final Label placeHolderLabel = new AspectCorrectLabel("?", aspectRatio, orientation);
      private final BiasedImageView imageView = new BiasedImageView(placeHolderLabel);
      private final AsyncImageProperty2 asyncImageProperty = new AsyncImageProperty2(400, 400);
      private final Label name = Labels.create("name");
      private final Label detail = Labels.create("detail");
      private final Label indicator = Labels.create("indicator");
      private final Label indicatorBackground = Labels.create("indicator-background");
      private final StackPane indicatorPane = new StackPane(indicatorBackground, indicator);
      private final Label sequenceNumber = Labels.create("sequence-number");
      private final StackPane sequenceNumberPane = new StackPane(sequenceNumber);
      private final ObjectProperty<MediaStatus> mediaStatusProperty = new SimpleObjectProperty<>();
      private final DropShadow dropShadow = new DropShadow(BlurType.GAUSSIAN, new Color(1, 1, 1, 0.8), 0, 0.0, 0, 0);

      private final VBox vbox = new VBox() {{
        getChildren().add(imageView);
        getChildren().add(name);
        getChildren().add(detail);

        getStyleClass().add("inner-box");

        setAlignment(Pos.CENTER);
        imageView.setOrientation(orientation);
        imageView.setMinRatio(minRatio);
        imageView.setMaxRatio(maxRatio);

        name.setMinHeight(Region.USE_PREF_SIZE);  // Always fit entire (reflowed) text

        if(orientation == Orientation.VERTICAL) {
          VBox.setVgrow(imageView, Priority.ALWAYS);
        }
        else {
          this.setMaxHeight(1);
        }
      }};

      private final ChangeListener<? super MediaStatus> updateIndicatorListener = (obs, old, current) -> {
        switch(current == null ? MediaStatus.UNAVAILABLE : current) {
        case WATCHED:
          getStyleClass().add("watched");
          getStyleClass().remove("available");
          getStyleClass().remove("unavailable");
          break;
        case AVAILABLE:
          getStyleClass().add("available");
          getStyleClass().remove("watched");
          getStyleClass().remove("unavailable");
          break;
        case UNAVAILABLE:
          getStyleClass().remove("available");
          getStyleClass().remove("watched");
          getStyleClass().add("unavailable");
          break;
        }
      };

      {
        imageView.setPreserveRatio(true);
        imageView.imageProperty().bind(asyncImageProperty);
        imageView.setAlignment(Pos.BOTTOM_CENTER);
        imageView.getOverlayRegion().getChildren().add(indicatorPane);
        imageView.getOverlayRegion().getChildren().add(sequenceNumberPane);

        placeHolderLabel.getStyleClass().add("media-grid-view-image-place-holder");
        placeHolderLabel.setAlignment(Pos.CENTER);

        sequenceNumberPane.setAlignment(Pos.BOTTOM_LEFT);
        sequenceNumberPane.getStyleClass().add("sequence-number-pane");
        indicatorPane.setAlignment(Pos.BOTTOM_RIGHT);
        indicatorPane.getStyleClass().add("indicator-pane");

        detail.managedProperty().bind(detail.textProperty().isNotEqualTo(""));
        sequenceNumber.managedProperty().bind(sequenceNumber.textProperty().isNotEqualTo(""));

        setAlignment(Pos.CENTER);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);  // Indicate to cell that it can become as big as possible

        imageView.setEffect(dropShadow);

        focusedProperty().addListener((obs, old, current) -> {
          new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(dropShadow.radiusProperty(), dropShadow.getRadius())),
            new KeyFrame(Duration.seconds(0.2), new KeyValue(dropShadow.radiusProperty(), current ? 25.0 : 0))
          ).play();
        });

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
      protected void updateItem(MediaItem<T> item, boolean empty) {
        super.updateItem(item, empty);

        asyncImageProperty.removeListener(imageChangeListener);

        if(!empty) {
          setGraphic(vbox);

          name.textProperty().bind(titleBindProvider.apply(item));
          asyncImageProperty.imageHandleProperty().set(imageHandleExtractor.apply(item));

          if(asyncImageProperty.get() == null) {
            asyncImageProperty.addListener(imageChangeListener);
          }

          if(detailExtractor != null) {
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
