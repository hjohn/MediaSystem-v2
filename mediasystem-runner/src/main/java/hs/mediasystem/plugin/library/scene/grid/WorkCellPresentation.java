package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Work;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.control.ListView;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Var;

public class WorkCellPresentation implements Presentation {
  private final WorkClient workClient;
  private final ListView<?> listView;

  @Singleton
  public static class Factory {
    @Inject private WorkClient workClient;

    public WorkCellPresentation create(ListView<?> listView) {
      return new WorkCellPresentation(workClient, listView);
    }
  }

  private WorkCellPresentation(WorkClient workClient, ListView<?> listView) {
    this.workClient = workClient;
    this.listView = listView;
  }

  public Var<Boolean> watchedProperty() {
    Object obj = listView.getSelectionModel().getSelectedItem();

    if(obj instanceof Work) {
      Work work = (Work)obj;

      return work.getState().isConsumed();
    }

    return null;  // Indicates no state possible as there is no stream
  }

  public Task<Void> reidentify(Event event) {
    event.consume();

    Object obj = listView.getSelectionModel().getSelectedItem();

    if(obj instanceof Work) {
      Work work = (Work)obj;

      if(!work.getStreams().isEmpty()) {
        return new Task<>() {
          @Override
          protected Void call() throws Exception {
            workClient.reidentify(work.getId());

            return null;
          }
        };

        // TODO after reidentify reload
        // 1) Replace item in list (or reload entire thing)
        // 2) Position may jump, depending on sorting
        // 3) Remember, task method may be called async...
      }
    }

    return null;
  }
}
