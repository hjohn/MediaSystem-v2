package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.runner.util.DebugFX;
import hs.mediasystem.util.javafx.Events;
import hs.mediasystem.util.javafx.ItemSelectedEvent;
import hs.mediasystem.util.javafx.control.gridlistviewskin.GridListViewSkin;
import hs.mediasystem.util.javafx.control.gridlistviewskin.GridListViewSkin.GroupDisplayMode;
import hs.mediasystem.util.javafx.control.gridlistviewskin.Group;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

/**
 * Cell based view, similar to ListView, which supports multiple columns of items
 * per row. Cells must all be of the same size.<p>
 *
 * Supports "jump points", which splits items in groups (causing a row to not
 * be completely filled), and optionally puts a header in between groups.
 *
 * @param <T> item type
 */
public class MediaGridView<T> extends ListView<T> {
  private static final KeyCombination ENTER = new KeyCodeCombination(KeyCode.ENTER);

  public final ObjectProperty<EventHandler<ItemSelectedEvent<T>>> onItemSelected = new SimpleObjectProperty<>();
  public final IntegerProperty visibleColumns = new SimpleIntegerProperty(4);
  public final IntegerProperty visibleRows = new SimpleIntegerProperty(3);
  public final ObjectProperty<List<Group>> groups = new SimpleObjectProperty<>();
  public final BooleanProperty scrollBarVisible = new SimpleBooleanProperty(true);

  /**
   * Show the headers in between groups.
   */
  public final BooleanProperty showHeaders = new SimpleBooleanProperty(true);

  /**
   * When true, skipping to the next page will instead skip to the end of the
   * current group, or to the start of the next group if already at the end.
   * Skipping to the previous page will skip to the start of the current group
   * or to the start of the previous group if already at the start.
   */
  public final BooleanProperty pageByGroup = new SimpleBooleanProperty(false);

  /**
   * The {@link GroupDisplayMode}.
   */
  public final ObjectProperty<GroupDisplayMode> groupDisplayMode = new SimpleObjectProperty<>(GroupDisplayMode.NONE);

  public MediaGridView() {
    DebugFX.addReference(this);

    GridListViewSkin skin = new GridListViewSkin(this);

    this.setSkin(skin);

    skin.visibleColumns.bindBidirectional(visibleColumns);
    skin.visibleRows.bindBidirectional(visibleRows);
    skin.groups.bindBidirectional(groups);
    skin.scrollBarVisible.bindBidirectional(scrollBarVisible);
    skin.pageByGroup.bindBidirectional(pageByGroup);
    skin.groupDisplayMode.bindBidirectional(groupDisplayMode);
    skin.showHeaders.bindBidirectional(showHeaders);

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

    getStyleClass().add("media-grid-view");
  }

  private void onItemSelected(Event event) {
    T focusedItem = MediaGridView.this.getFocusModel().getFocusedItem();

    if(focusedItem != null) {
      Events.dispatchEvent(onItemSelected, new ItemSelectedEvent<>(event.getTarget(), focusedItem), event);
      event.consume();
    }
  }
}
