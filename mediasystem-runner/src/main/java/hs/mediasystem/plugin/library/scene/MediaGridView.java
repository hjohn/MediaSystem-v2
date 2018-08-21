package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.runner.DebugFX;
import hs.mediasystem.util.javafx.Events;
import hs.mediasystem.util.javafx.GridListViewSkin;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class MediaGridView<T> extends ListView<T> {
  private static final KeyCombination ENTER = new KeyCodeCombination(KeyCode.ENTER);

  public final ObjectProperty<EventHandler<ItemSelectedEvent<T>>> onItemSelected = new SimpleObjectProperty<>();
  public final IntegerProperty visibleColumns = new SimpleIntegerProperty(4);
  public final IntegerProperty visibleRows = new SimpleIntegerProperty(3);
  public final ObjectProperty<List<Integer>> jumpPoints = new SimpleObjectProperty<>();

  public MediaGridView() {
    DebugFX.addReference(this);

    GridListViewSkin skin = new GridListViewSkin(this);

    this.setSkin(skin);

    skin.visibleColumns.bindBidirectional(visibleColumns);
    skin.visibleRows.bindBidirectional(visibleRows);
    skin.jumpPoints.bindBidirectional(jumpPoints);

    this.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent event) {
        if(event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
          onItemSelected(event);
        }
      }
    });

    this.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
      @Override
      public void handle(KeyEvent event) {
        if(ENTER.match(event)) {
          onItemSelected(event);
        }
      }
    });

  //gridView.setCellWidth(300);
  //gridView.setCellHeight(400);
  //gridView.getSelectionModel().selectedItemProperty().addListener(this::navigateFromListToMedia);
    this.getStyleClass().add("media-grid-view");
  }

  private void onItemSelected(Event event) {
    T focusedItem = MediaGridView.this.getFocusModel().getFocusedItem();

    if(focusedItem != null) {
      Events.dispatchEvent(onItemSelected, new ItemSelectedEvent<>(event.getTarget(), focusedItem), event);
      event.consume();
    }
  }
}
