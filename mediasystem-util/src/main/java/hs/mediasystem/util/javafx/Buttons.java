package hs.mediasystem.util.javafx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

public class Buttons {
  public static Button create(String text, EventHandler<ActionEvent> eventHandler) {
    Button button = new Button(text);

    button.setOnAction(eventHandler);

    return button;
  }

  public static Button create(Action action) {
    Button button = new Button();

    button.textProperty().bind(action.titleProperty());
    button.disableProperty().bind(action.enabledProperty().map(b -> !b));
    button.setOnAction(action::trigger);

    return button;
  }
}
