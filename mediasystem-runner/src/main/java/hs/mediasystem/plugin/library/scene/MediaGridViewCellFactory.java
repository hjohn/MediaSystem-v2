package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.javafx.AsyncImageProperty2;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.VerticalLabel;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VerticalDirection;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Duration;

public class MediaGridViewCellFactory<T> implements Callback<ListView<T>, ListCell<T>> {
  private static final List<String> MEDIA_STATE_STYLES = List.of("watched", "available", "unavailable");

  public interface Binder<T> extends BinderBase<T> {
    Function<T, ObservableValue<? extends String>> titleBindProvider();
    Function<T, ImageHandle> imageHandleExtractor();

    default Function<T, ObservableValue<? extends String>> sideBarTopLeftBindProvider() { return null; }
    default Function<T, ObservableValue<? extends String>> sideBarCenterBindProvider() { return null; }
    default Function<T, String> detailExtractor() { return null; }
    default Function<T, String> sequenceNumberExtractor() { return null; }
    default Optional<BooleanProperty> watchedProperty(@SuppressWarnings("unused") T item) { return Optional.empty(); }
    default Optional<Boolean> hasStream(@SuppressWarnings("unused") T item) { return Optional.empty(); }
  }

  private final BinderProvider bindersProvider;

  private Orientation orientation = Orientation.HORIZONTAL;
  private double aspectRatio = 1.5;

  public MediaGridViewCellFactory(BinderProvider bindersProvider) {
    this.bindersProvider = bindersProvider;
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
  public ListCell<T> call(ListView<T> param) {
    return new ListCell<>() {
      private final Label placeHolderLabel = new AspectCorrectLabel("?", aspectRatio, orientation, 1000, 1000);
      private final BiasedImageView imageView = new BiasedImageView(placeHolderLabel);
      private final AsyncImageProperty2 asyncImageProperty = new AsyncImageProperty2(400, 400);
      private final Label name = Labels.create("name");
      private final Label detail = Labels.create("detail");
      private final Label indicator = Labels.create("indicator");
      private final Label indicatorBackground = Labels.create("indicator-background");
      private final StackPane indicatorPane = new StackPane(indicatorBackground, indicator);
      private final Label sequenceNumber = Labels.create("sequence-number");
      private final StackPane sequenceNumberPane = new StackPane(sequenceNumber);
      private final BooleanProperty watchedProperty = new SimpleBooleanProperty();
      private final BooleanProperty hasStreamProperty = new SimpleBooleanProperty();
      private final DropShadow dropShadow = new DropShadow(BlurType.GAUSSIAN, new Color(1, 1, 1, 0.8), 0, 0.0, 0, 0);

      private final StringProperty sideBarTopLeftText = new SimpleStringProperty();
      private final StringProperty sideBarCenterText = new SimpleStringProperty();

      private final Pane sideDetail;

      {
        if(orientation == Orientation.VERTICAL) {
          sideDetail = new VBox();

          VerticalLabel topLeft = new VerticalLabel(VerticalDirection.DOWN);
          VerticalLabel center = new VerticalLabel(VerticalDirection.DOWN);

          sideDetail.getChildren().addAll(topLeft, center, indicatorPane);

          topLeft.textProperty().bind(sideBarTopLeftText);
          topLeft.getStyleClass().add("top-left");
          topLeft.setMinWidth(Region.USE_PREF_SIZE);
          topLeft.setMinHeight(Region.USE_PREF_SIZE);

          center.textProperty().bind(sideBarCenterText);
          center.getStyleClass().add("center");
          center.setMinWidth(Region.USE_PREF_SIZE);
          center.setMaxHeight(Double.MAX_VALUE);

          VBox.setVgrow(center, Priority.ALWAYS);
        }
        else {
          sideDetail = new HBox();

          Label topLeft = new Label();
          Label center = new Label();

          topLeft.textProperty().bind(sideBarTopLeftText);
          topLeft.getStyleClass().add("top-left");

          center.textProperty().bind(sideBarCenterText);
          center.getStyleClass().add("center");
          center.setMaxWidth(Double.MAX_VALUE);

          sideDetail.getChildren().addAll(topLeft, center, indicatorPane);

          HBox.setHgrow(center, Priority.ALWAYS);
        }

        sideDetail.getStyleClass().add("image-detail-bar");
      }

      private final Pane imageBox;

      {
        if(orientation == Orientation.VERTICAL) {
          imageBox = Containers.hbox("image-box,vertical", imageView, sideDetail);
        }
        else {
          imageBox = Containers.vbox("image-box,horizontal", imageView, sideDetail);
          VBox.setVgrow(imageView, Priority.ALWAYS);
        }
       // sideDetail.setPrefHeight(Double.MAX_VALUE);

//        hbox.setMaxHeight(Double.MAX_VALUE);
//hbox.setMinHeight(1);
//hbox.setPrefHeight(1E5);
//hbox.setFillHeight(true);
     //   HBox.setHgrow(imageView, Priority.ALWAYS);
      }

      private final VBox vbox = Containers.vbox("cell-box", new VBox() {{
        getChildren().add(imageBox);
        getChildren().add(name);
        getChildren().add(detail);

        getStyleClass().add("container-box");

        imageView.setOrientation(orientation);
        imageView.setMinRatio(minRatio);
        imageView.setMaxRatio(maxRatio);
//        imageView.setMaxWidth(Region.USE_PREF_SIZE);  // Fucks up horizontal version

        name.setMinHeight(Region.USE_PREF_SIZE);  // Always fit entire (reflowed) text
        detail.setMinHeight(Region.USE_PREF_SIZE);  // Always fit entire (reflowed) text

        if(orientation == Orientation.VERTICAL) {
//          VBox.setVgrow(imageView, Priority.ALWAYS);
          VBox.setVgrow(imageBox, Priority.ALWAYS);
//          VBox.setVgrow(detail, Priority.ALWAYS);
        }
        else {
          HBox.setHgrow(imageView, Priority.ALWAYS);
        //  this.setMaxHeight(1);
        }
      }});

      private final ChangeListener<Boolean> updateIndicatorListener = (obs, old, watched) -> updateMediaStateStyles();

      private void updateMediaStateStyles() {
        getStyleClass().removeAll(MEDIA_STATE_STYLES);

        if(watchedProperty.get()) {
          getStyleClass().add("watched");
        }
        else if(hasStreamProperty.get()) {
          getStyleClass().add("available");
        }
        else {
          getStyleClass().add("unavailable");
        }
      }

      {
        imageView.setPreserveRatio(true);
        imageView.imageProperty().bind(asyncImageProperty);
        imageView.setAlignment(Pos.BOTTOM_CENTER);
        //imageView.getOverlayRegion().getChildren().add(indicatorPane);
        imageView.getOverlayRegion().getChildren().add(sequenceNumberPane);

        placeHolderLabel.getStyleClass().add("media-grid-view-image-place-holder");
        placeHolderLabel.setAlignment(Pos.CENTER);

        sequenceNumberPane.setAlignment(Pos.BOTTOM_LEFT);
        sequenceNumberPane.getStyleClass().add("sequence-number-pane");
        //indicatorPane.setAlignment(Pos.BOTTOM_RIGHT);
        indicatorPane.getStyleClass().add("indicator-pane");

        detail.managedProperty().bind(detail.textProperty().isNotEqualTo(""));
        sequenceNumber.managedProperty().bind(sequenceNumber.textProperty().isNotEqualTo(""));

        setAlignment(Pos.CENTER);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);  // Indicate to cell that it can become as big as possible

        imageBox.setEffect(dropShadow);

        focusedProperty().addListener((obs, old, current) -> {
          new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(dropShadow.radiusProperty(), dropShadow.getRadius())),
            new KeyFrame(Duration.seconds(0.2), new KeyValue(dropShadow.radiusProperty(), current ? 25.0 : 0))
          ).play();
        });

        name.textProperty().addListener((obs, old, current) -> {
          if(current.length() >= 30) {
            name.getStyleClass().add("smaller");
          }
          else {
            name.getStyleClass().remove("smaller");
          }
        });

        this.hasStreamProperty.addListener(updateIndicatorListener);
        this.watchedProperty.addListener(updateIndicatorListener);
      }

      @Override
      protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if(!empty) {
          @SuppressWarnings("unchecked")
          Binder<T> binders = bindersProvider.findBinder(Binder.class, item.getClass()).orElseThrow(() -> new IllegalStateException("No binder available for class: " + item.getClass()));

          setGraphic(vbox);

          name.textProperty().bind(binders.titleBindProvider().apply(item));
          asyncImageProperty.imageHandleProperty().set(binders.imageHandleExtractor().apply(item));

          if(binders.detailExtractor() != null) {
            detail.setText(binders.detailExtractor().apply(item));
          }

          if(binders.sideBarTopLeftBindProvider() != null && binders.sideBarTopLeftBindProvider().apply(item) != null) {
            sideDetail.setVisible(true);
            sideDetail.setManaged(true);
            sideBarTopLeftText.bind(binders.sideBarTopLeftBindProvider().apply(item));
          }

          if(binders.sideBarCenterBindProvider() != null && binders.sideBarCenterBindProvider().apply(item) != null) {
            sideDetail.setVisible(true);
            sideDetail.setManaged(true);
            sideBarCenterText.bind(binders.sideBarCenterBindProvider().apply(item));
          }

          binders.hasStream(item).ifPresent(hasStreamProperty::set);
          binders.watchedProperty(item).ifPresent(watchedProperty::bind);

          if(binders.sequenceNumberExtractor() != null) {
            sequenceNumber.setText(binders.sequenceNumberExtractor().apply(item));
          }

          updateMediaStateStyles();
        }
        else {
          setGraphic(null);

          sideDetail.setVisible(false);
          sideDetail.setManaged(false);

          name.textProperty().unbind();
          sideBarTopLeftText.unbind();
          sideBarCenterText.unbind();
          watchedProperty.unbind();
          hasStreamProperty.set(true);
          asyncImageProperty.imageHandleProperty().set(null);  // Helps to cancel bg loading of images when cells quickly change
          getStyleClass().removeAll(MEDIA_STATE_STYLES);
        }
      }
    };
  }
}
