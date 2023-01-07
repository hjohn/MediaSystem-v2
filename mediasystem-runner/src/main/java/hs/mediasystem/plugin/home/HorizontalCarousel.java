package hs.mediasystem.plugin.home;

import hs.mediasystem.util.javafx.base.ItemSelectedEvent;
import hs.mediasystem.util.javafx.control.ActionListView;
import hs.mediasystem.util.javafx.ui.carousel.CarouselSkin;
import hs.mediasystem.util.javafx.ui.carousel.LinearLayout;

import java.util.List;
import java.util.function.Consumer;

import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

public class HorizontalCarousel<T> extends ActionListView<T> {

  public HorizontalCarousel(List<T> items, Consumer<ItemSelectedEvent<T>> onSelect, Callback<ListView<T>, ListCell<T>> cellFactory) {
    LinearLayout layout = new LinearLayout();

    layout.centerPositionProperty().set(0.2);
    layout.maxCellWidthProperty().set(350);
    layout.maxCellHeightProperty().set(300);
    layout.densityProperty().set(0.0026);
    layout.reflectionEnabledProperty().set(true);
    layout.cellAlignmentProperty().set(1.0);
    layout.clipReflectionsProperty().set(true);
    layout.viewAlignmentProperty().set(0.9);

    CarouselSkin<T> skin = new CarouselSkin<>(this);

    skin.setLayout(layout);
    skin.verticalAlignmentProperty().set(0.6);

    setCellFactory(cellFactory);
    setItems(FXCollections.observableList(items));
    getSelectionModel().select(0);
    getFocusModel().focus(0);
    setOrientation(Orientation.HORIZONTAL);
    onItemSelected.set(onSelect::accept);
    setSkin(skin);
  }
}
