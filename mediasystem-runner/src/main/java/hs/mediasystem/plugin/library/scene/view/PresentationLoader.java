package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.runner.util.Dialogs;

import java.util.function.Supplier;

import javafx.concurrent.Task;
import javafx.event.Event;

public class PresentationLoader {

  public static <T extends Presentation> void navigate(Event event, Supplier<T> supplier) {
    Dialogs.showProgressDialog(event, new Task<T>() {
      @Override
      protected T call() throws Exception {
        updateTitle("Loading...");

        return supplier.get();
      }
    }).ifPresent(p -> Event.fireEvent(event.getTarget(), NavigateEvent.to(p)));

    event.consume();
  }
}
