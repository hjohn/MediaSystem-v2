package hs.mediasystem.runner.presentation;

import hs.mediasystem.presentation.PresentationEvent;
import hs.mediasystem.runner.dialog.Dialogs;

import javafx.event.Event;
import javafx.scene.Node;

public class Presentations {

  public static void showWindow(Event event, Node content) {
    Dialogs.show(event, "", content);

    if(event.getTarget() instanceof Node node) {
      node.fireEvent(PresentationEvent.refresh());
    }
  }

  public static void showDialog(Event event, Node content) {
    Dialogs.show(event, content);

    if(event.getTarget() instanceof Node node) {
      node.fireEvent(PresentationEvent.refresh());
    }
  }
}
