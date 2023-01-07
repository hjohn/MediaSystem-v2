package hs.mediasystem.plugin.cell;

import hs.mediasystem.util.image.ImageHandle;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.ui.carousel.CarouselListCell;
import hs.mediasystem.util.javafx.ui.csslayout.StylableContainers;
import hs.mediasystem.util.javafx.ui.csslayout.StylableHBox;
import hs.mediasystem.util.javafx.ui.status.StatusIndicator;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import javafx.util.Duration;

public class AnnotatedImageCellFactory<T> implements Callback<ListView<T>, ListCell<T>> {
  private final BiConsumer<T, Model> updater;

  public static class Model {
    public final StringProperty parentTitle = new SimpleStringProperty();
    public final StringProperty title = new SimpleStringProperty();
    public final StringProperty subtitle = new SimpleStringProperty();
    public final StringProperty sequence = new SimpleStringProperty();
    public final StringProperty age = new SimpleStringProperty();
    public final ObjectProperty<ImageHandle> imageHandle = new SimpleObjectProperty<>();
    public final DoubleProperty watchedFraction = new SimpleDoubleProperty();

    private void clear() {
      parentTitle.unbind();
      title.unbind();
      subtitle.unbind();
      sequence.unbind();
      age.unbind();
      imageHandle.unbind();
      watchedFraction.unbind();

      parentTitle.set("");
      title.set("");
      subtitle.set("");
      sequence.set("");
      age.set("");
      imageHandle.set(null);
      watchedFraction.set(-1);
    }
  }

  public AnnotatedImageCellFactory(BiConsumer<T, Model> updater) {
    this.updater = updater;
  }

  private static class LengthChangeListener implements InvalidationListener {
    final List<Label> labels;

    LengthChangeListener(Label... labels) {
      this.labels = Arrays.asList(labels);
    }

    @Override
    public void invalidated(Observable observable) {
      int longestLength = labels.stream().map(Label::getText).filter(Objects::nonNull).mapToInt(String::length).max().orElse(0);
      String styleClass = longestLength >= 45 ? "smaller2" : longestLength >= 30 ? "smaller" : null;

      for(Label label : labels) {
        ObservableList<String> styleClasses = label.getStyleClass();

        styleClasses.removeAll("smaller", "smaller2");

        if(styleClass != null) {
          styleClasses.add(styleClass);
        }
      }
    }
  }

  private static final void dynamicSize(Label... labels) {
    LengthChangeListener listener = new LengthChangeListener(labels);

    for(Label label : labels) {
      label.textProperty().addListener(listener);
    }
  }

  @Override
  public CarouselListCell<T> call(ListView<T> view) {
    return new CarouselListCell<>() {
      private final Model model = new Model();

      private final BiasedImageView imageView = new BiasedImageView();
      private final AsyncImageProperty asyncImageProperty = new AsyncImageProperty(900, 600);

      private final StatusIndicator indicator = new StatusIndicator();
      private final Label parentTitle = Labels.create("parent-title", Labels.HIDE_IF_EMPTY);
      private final Label title = Labels.create("title");
      private final Label subtitle = Labels.create("subtitle", Labels.HIDE_IF_EMPTY);
      private final Label sequence = Labels.create("sequence", Labels.HIDE_IF_EMPTY);
      private final Label age = Labels.create("age", Labels.HIDE_IF_EMPTY);

      private final StylableHBox overlay = StylableContainers.hbox("overlay", indicator, parentTitle, title, subtitle, sequence, age);

      private Timeline activeTimeline;

      {
        imageView.setOrientation(Orientation.HORIZONTAL);
        imageView.imageProperty().bind(asyncImageProperty);
        imageView.getOverlayRegion().getChildren().add(overlay);
        imageView.setPrefWidth(600);
        imageView.setMinRatio(4.0 / 3.0);
        imageView.setAlignment(Pos.CENTER);

        indicator.showPercentage.setValue(false);

        StackPane.setAlignment(overlay, Pos.BOTTOM_CENTER);

        overlay.setMaxHeight(Region.USE_PREF_SIZE);

        focusedProperty().addListener((obs, old, current) -> {
          if(activeTimeline != null) {
            activeTimeline.stop();
          }

          activeTimeline = current
            ? new Timeline(new KeyFrame(Duration.seconds(0.5), new KeyValue(zoomProperty(), 1.5)))
            : new Timeline(new KeyFrame(Duration.seconds(0.5), new KeyValue(zoomProperty(), 1.0)));

          activeTimeline.play();
        });

        dynamicSize(parentTitle, title, subtitle);

        parentTitle.textProperty().bind(model.parentTitle);
        title.textProperty().bind(model.title);
        subtitle.textProperty().bind(model.subtitle);
        asyncImageProperty.imageHandleProperty().bind(model.imageHandle);
        indicator.value.bind(model.watchedFraction);
        age.textProperty().bind(model.age);
        sequence.textProperty().bind(model.sequence);
      }

      @Override
      protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if(!empty) {
          setGraphic(imageView);

          updater.accept(item, model);
        }
        else {
          setGraphic(null);

          model.clear();
        }
      }
    };
  }
}