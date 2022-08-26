package hs.mediasystem.plugin.cell;

import hs.mediasystem.plugin.library.scene.MediaStatus;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.VerticalLabel;
import hs.mediasystem.util.javafx.control.VerticalLabels;
import hs.mediasystem.util.javafx.control.csslayout.StylableContainers;
import hs.mediasystem.util.javafx.control.csslayout.StylableVBox;

import java.util.List;
import java.util.function.BiConsumer;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

public class MediaGridViewCellFactory<T> implements Callback<ListView<T>, ListCell<T>> {
  private static final List<String> MEDIA_STATE_STYLES = List.of("watched", "available", "unavailable");

  public static class Model {
    public final StringProperty title = new SimpleStringProperty();
    public final StringProperty subtitle = new SimpleStringProperty();
    public final StringProperty annotation1 = new SimpleStringProperty();
    public final StringProperty annotation2 = new SimpleStringProperty();
    public final ObjectProperty<ImageHandle> imageHandle = new SimpleObjectProperty<>();
    public final ObjectProperty<MediaStatus> status = new SimpleObjectProperty<>();

    private void clear() {
      title.unbind();
      subtitle.unbind();
      annotation1.unbind();
      annotation2.unbind();
      imageHandle.unbind();
      status.unbind();

      title.set(null);
      subtitle.set(null);
      annotation1.set(null);
      annotation2.set(null);
      imageHandle.set(null);
      status.set(null);
    }
  }

  private final BiConsumer<T, Model> updater;

  private Orientation orientation = Orientation.HORIZONTAL;
  private double aspectRatio = 2.0 / 3.0;
  private double maxRatio = Double.MAX_VALUE;
  private double minRatio = 0;

  public MediaGridViewCellFactory(BiConsumer<T, Model> updater) {
    this.updater = updater;
  }

  public void setContentBias(Orientation orientation) {
    this.orientation = orientation;
  }

  public void setPlaceHolderAspectRatio(double aspectRatio) {
    this.aspectRatio = aspectRatio;
  }

  public void setMinRatio(double minRatio) {
    this.minRatio = minRatio;
  }

  public void setMaxRatio(double maxRatio) {
    this.maxRatio = maxRatio;
  }

  @Override
  public ListCell<T> call(ListView<T> param) {
    return new ListCell<>() {
      private final Model model = new Model();
      private final BooleanProperty allowUpdates = new SimpleBooleanProperty(true);

      private final Label placeHolderLabel = Labels.create("place-holder", "?");
      private final Label name = Labels.create("name");
      private final Label detail = Labels.create("detail", Labels.HIDE_IF_EMPTY);
      private final StackPane indicatorPane = Containers.stack("indicator-pane", Labels.create("indicator-background"), Labels.create("indicator"));
      private final VerticalLabel extraVertical1 = VerticalLabels.create("extra-vertical-1", VerticalLabels.HIDE_IF_EMPTY);
      private final VerticalLabel extraVertical2 = VerticalLabels.create("extra-vertical-2", VerticalLabels.HIDE_IF_EMPTY);
      private final Label extraHorizontal1 = Labels.create("extra-horizontal-1", Labels.HIDE_IF_EMPTY);
      private final Label extraHorizontal2 = Labels.create("extra-horizontal-2", Labels.HIDE_IF_EMPTY);

      private final BiasedImageView imageView;
      private final StylableVBox container;

      private final AsyncImageProperty asyncImageProperty = new AsyncImageProperty(800, 600);  // TODO should really make this depend on actual space available
      private final StringProperty sideBarTopLeftText = new SimpleStringProperty();
      private final StringProperty sideBarCenterText = new SimpleStringProperty();
      private final ObjectProperty<MediaStatus> mediaStatusProperty = new SimpleObjectProperty<>();

      private final ChangeListener<MediaStatus> updateIndicatorListener = (obs, old, watched) -> updateMediaStateStyles();

      {
        imageView = new BiasedImageView(placeHolderLabel, aspectRatio);

        imageView.setMinRatio(minRatio);
        imageView.setMaxRatio(maxRatio);
        imageView.setOrientation(orientation);
        imageView.setPreserveRatio(true);
        imageView.imageProperty().bind(asyncImageProperty);
        imageView.setAlignment(Pos.CENTER);  // mainly to get placeholder to be in the center

        container = StylableContainers.vbox(
          "container",
          imageView, name, detail, indicatorPane, extraHorizontal1, extraHorizontal2, extraVertical1, extraVertical2
        );

        name.setMinHeight(Region.USE_PREF_SIZE);  // Always fit entire (reflowed) text
        detail.setMinHeight(Region.USE_PREF_SIZE);  // Always fit entire (reflowed) text

        // first extra label doesn't reflow, force it to always display full:
        extraHorizontal1.setMinWidth(Region.USE_PREF_SIZE);
        extraVertical1.setMinHeight(Region.USE_PREF_SIZE);

        // second extra label does reflow, force it to always get enough lines to display full:
        extraHorizontal2.setMinHeight(Region.USE_PREF_SIZE);
        extraVertical2.setMinWidth(Region.USE_PREF_SIZE);

        extraHorizontal1.textProperty().bind(sideBarTopLeftText);
        extraHorizontal2.textProperty().bind(sideBarCenterText);
        extraVertical1.textProperty().bind(sideBarTopLeftText);
        extraVertical2.textProperty().bind(sideBarCenterText);

        container.getStyleClass().add(orientation == Orientation.VERTICAL ? "vertical" : "horizontal");

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);  // Indicate to cell that it can become as big as possible

        name.textProperty().addListener((obs, old, current) -> {
          if(current != null && current.length() >= 30) {
            name.getStyleClass().add("smaller");
          }
          else {
            name.getStyleClass().remove("smaller");
          }
        });

        this.mediaStatusProperty.addListener(updateIndicatorListener);
      }

      {
        BooleanBinding sideBarVisible = model.annotation1.isNotEmpty().or(model.annotation2.isNotEmpty());

        indicatorPane.managedProperty().bind(sideBarVisible);
        indicatorPane.visibleProperty().bind(sideBarVisible);

        name.textProperty().bind(model.title.conditionOn(allowUpdates));
        detail.textProperty().bind(model.subtitle.conditionOn(allowUpdates));
        asyncImageProperty.imageHandleProperty().bind(model.imageHandle.conditionOn(allowUpdates));
        sideBarTopLeftText.bind(model.annotation1.conditionOn(allowUpdates));
        sideBarCenterText.bind(model.annotation2.conditionOn(allowUpdates));
        mediaStatusProperty.bind(model.status.conditionOn(allowUpdates));
      }

      private void updateMediaStateStyles() {
        getStyleClass().removeAll(MEDIA_STATE_STYLES);

        if(mediaStatusProperty.get() == MediaStatus.WATCHED) {
          getStyleClass().add("watched");
        }
        else if(mediaStatusProperty.get() == MediaStatus.UNAVAILABLE) {
          getStyleClass().add("unavailable");
        }
        else {
          getStyleClass().add("available");
        }
      }

      @Override
      protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if(!empty) {
          allowUpdates.set(false);

          try {
            setGraphic(container);

            model.clear();
            updater.accept(item, model);
          }
          finally {
            allowUpdates.set(true);
          }
        }
        else {
          setGraphic(null);

          model.clear();
        }
      }
    };
  }
}
