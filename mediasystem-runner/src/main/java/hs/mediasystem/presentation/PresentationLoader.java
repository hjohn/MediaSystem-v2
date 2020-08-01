package hs.mediasystem.presentation;

import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.runner.util.Dialogs;

import java.util.function.Supplier;

import javafx.concurrent.Task;
import javafx.event.Event;

public class PresentationLoader {

  public static <T extends Presentation> void navigate(Event event, Supplier<T> presentationSupplier) {
    Dialogs.showProgressDialog(event, new Task<T>() {
      @Override
      protected T call() throws Exception {
        updateTitle("Loading...");

        return presentationSupplier.get();
      }
    }).ifPresent(p -> Event.fireEvent(event.getTarget(), NavigateEvent.to(p)));

    event.consume();
  }

  public static <T extends Presentation> void navigate(Event event, Task<T> task) {
    Dialogs.showProgressDialog(event, task).ifPresent(p -> Event.fireEvent(event.getTarget(), NavigateEvent.to(p)));

    event.consume();
  }
}
