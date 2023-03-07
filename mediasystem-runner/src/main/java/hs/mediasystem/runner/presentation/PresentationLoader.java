package hs.mediasystem.runner.presentation;

import hs.mediasystem.presentation.NavigateEvent;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.dialog.Dialogs;

import java.util.function.Supplier;

import javafx.concurrent.Task;
import javafx.event.Event;

public class PresentationLoader {

  public static <T extends Presentation> void navigate(Event event, Supplier<T> presentationSupplier) {
    navigate(event, new Task<T>() {
      @Override
      protected T call() throws Exception {
        updateTitle("Loading...");

        return presentationSupplier.get();
      }
    });
  }

  public static <T extends Presentation> void navigate(Event event, Task<T> task) {
    Dialogs.showProgressDialog(event, task).ifPresent(p -> Event.fireEvent(event.getTarget(), NavigateEvent.to(p)));

    event.consume();
  }
}
