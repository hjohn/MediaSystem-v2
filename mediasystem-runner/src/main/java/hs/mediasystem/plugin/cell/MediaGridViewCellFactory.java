package hs.mediasystem.plugin.cell;

import hs.mediasystem.plugin.library.scene.BinderBase;
import hs.mediasystem.plugin.library.scene.BinderProvider;
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
import java.util.function.Function;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

public class MediaGridViewCellFactory<T> implements Callback<ListView<T>, ListCell<T>> {
  private static final List<String> MEDIA_STATE_STYLES = List.of("watched", "available", "unavailable");

  public interface Binder<T> extends BinderBase<T> {
    Function<T, ObservableValue<? extends String>> titleBindProvider();
    Function<T, ImageHandle> imageHandleExtractor();

    default Function<T, ObservableValue<? extends String>> sideBarTopLeftBindProvider() { return null; }
    default Function<T, ObservableValue<? extends String>> sideBarCenterBindProvider() { return null; }
    default Function<T, String> detailExtractor() { return null; }
    default boolean watchedProperty(@SuppressWarnings("unused") T item) { return false; }
    default boolean hasStream(@SuppressWarnings("unused") T item) { return false; }
  }

  private final BinderProvider bindersProvider;

  private Orientation orientation = Orientation.HORIZONTAL;
  private double aspectRatio = 2.0 / 3.0;
  private double maxRatio = Double.MAX_VALUE;
  private double minRatio = 0;

  public MediaGridViewCellFactory(BinderProvider bindersProvider) {
    this.bindersProvider = bindersProvider;
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
      private final Label placeHolderLabel = Labels.create("place-holder", "");
      private final Label name = Labels.create("name");
      private final Label detail = Labels.create("detail", Labels.HIDE_IF_EMPTY);
      private final StackPane indicatorPane = Containers.stack("indicator-pane", Labels.create("indicator-background"), Labels.create("indicator"));
      private final VerticalLabel extraVertical1 = VerticalLabels.create("extra-vertical-1", VerticalLabels.HIDE_IF_EMPTY);
      private final VerticalLabel extraVertical2 = VerticalLabels.create("extra-vertical-2", VerticalLabels.HIDE_IF_EMPTY);
      private final Label extraHorizontal1 = Labels.create("extra-horizontal-1", Labels.HIDE_IF_EMPTY);
      private final Label extraHorizontal2 = Labels.create("extra-horizontal-2", Labels.HIDE_IF_EMPTY);

      private final BiasedImageView imageView;
      private final StylableVBox container;

      private final AsyncImageProperty asyncImageProperty = new AsyncImageProperty(400, 400);
      private final StringProperty sideBarTopLeftText = new SimpleStringProperty();
      private final StringProperty sideBarCenterText = new SimpleStringProperty();
      private final BooleanProperty watchedProperty = new SimpleBooleanProperty();
      private final BooleanProperty hasStreamProperty = new SimpleBooleanProperty();

      private final ChangeListener<Boolean> updateIndicatorListener = (obs, old, watched) -> updateMediaStateStyles();

      {
        imageView = new BiasedImageView(placeHolderLabel, aspectRatio);

        imageView.setMinRatio(minRatio);
        imageView.setMaxRatio(maxRatio);
        imageView.setOrientation(orientation);
        imageView.setPreserveRatio(true);
        imageView.imageProperty().bind(asyncImageProperty);

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

      @Override
      protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if(!empty) {
          @SuppressWarnings("unchecked")
          Binder<T> binders = bindersProvider.findBinder(Binder.class, item.getClass()).orElseThrow(() -> new IllegalStateException("No binder available for class: " + item.getClass()));

          setGraphic(container);

          name.textProperty().bind(binders.titleBindProvider().apply(item));
          asyncImageProperty.imageHandleProperty().set(binders.imageHandleExtractor().apply(item));
          detail.setText(binders.detailExtractor() == null ? null : binders.detailExtractor().apply(item));

          boolean sideBarVisible = false;

          if(binders.sideBarTopLeftBindProvider() != null && binders.sideBarTopLeftBindProvider().apply(item) != null) {
            sideBarVisible = true;
            sideBarTopLeftText.bind(binders.sideBarTopLeftBindProvider().apply(item));
          }
          else {
            sideBarTopLeftText.unbind();
          }

          if(binders.sideBarCenterBindProvider() != null && binders.sideBarCenterBindProvider().apply(item) != null) {
            sideBarVisible = true;
            sideBarCenterText.bind(binders.sideBarCenterBindProvider().apply(item));
          }
          else {
            sideBarCenterText.unbind();
          }

          hasStreamProperty.set(binders.hasStream(item));
          watchedProperty.set(binders.watchedProperty(item));

          indicatorPane.setManaged(sideBarVisible);
          indicatorPane.setVisible(sideBarVisible);

          updateMediaStateStyles();
        }
        else {
          setGraphic(null);

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
