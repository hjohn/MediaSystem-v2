package hs.mediasystem.util.javafx.control;

import hs.mediasystem.util.javafx.Events;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import javafx.beans.property.ObjectProperty;
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

public class ActionListView<T> extends ListView<T> {
  private static final KeyCombination ENTER = new KeyCodeCombination(KeyCode.ENTER);

  public final ObjectProperty<EventHandler<ItemSelectedEvent<T>>> onItemSelected = new SimpleObjectProperty<>();

  public ActionListView() {
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
  }

  private void onItemSelected(Event event) {
    T focusedItem = ActionListView.this.getFocusModel().getFocusedItem();

    if(focusedItem != null) {
      Events.dispatchEvent(onItemSelected, new ItemSelectedEvent<>(event.getTarget(), focusedItem), event);
      event.consume();
    }
  }
}
