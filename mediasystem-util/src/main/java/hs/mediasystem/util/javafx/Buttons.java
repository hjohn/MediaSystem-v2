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
}
