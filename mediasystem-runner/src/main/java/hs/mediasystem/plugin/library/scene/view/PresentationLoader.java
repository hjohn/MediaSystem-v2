package hs.mediasystem.plugin.library.scene.view;

import hs.ddif.core.Injector;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.Dialogs;
import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.util.Exceptional;

import java.util.function.Supplier;

import javafx.concurrent.Task;
import javafx.event.Event;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PresentationLoader {
  @Inject private Injector injector;

  public <T extends Presentation> void loadAndNavigate(Event event, PresentationSetupTask<T> setupTask) {
    // TODO exceptional needed here still?
    Exceptional<T> result = Dialogs.showProgressDialog(event, new Task<T>() {
      {
        setupTask.titleProperty().addListener((ov, old, current) -> updateTitle(current));
        setupTask.messageProperty().addListener((ov, old, current) -> updateMessage(current));
        setupTask.progressProperty().addListener((ov, old, current) -> updateProgress(current.doubleValue(), 1.0));
      }

      @Override
      protected T call() throws Exception {
        T presentation = injector.getInstance(setupTask.getPresentationClass());

        setupTask.setPresentation(presentation);
        setupTask.call();

        return presentation;
      }
    });

    result.ifPresent(p -> Event.fireEvent(event.getTarget(), NavigateEvent.to(p)));

    event.consume();
  }

  public <T extends Presentation> void loadAndNavigate(Event event, Supplier<T> supplier) {
    Exceptional<T> result = Dialogs.showProgressDialog(event, new Task<T>() {
      @Override
      protected T call() throws Exception {
        updateTitle("Loading...");

        return supplier.get();
      }
    });

    result.ifPresent(p -> Event.fireEvent(event.getTarget(), NavigateEvent.to(p)));

    event.consume();
  }
}
