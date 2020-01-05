package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.client.Work;
import hs.mediasystem.db.StreamCacheUpdateService;
import hs.mediasystem.ext.basicmediatypes.domain.stream.MediaStream;
import hs.mediasystem.presentation.Presentation;

import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.control.ListView;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Var;

public class WorkCellPresentation implements Presentation {
  private final StreamCacheUpdateService updateService;
  private final ListView<?> listView;

  @Singleton
  public static class Factory {
    @Inject private StreamCacheUpdateService updateService;

    public WorkCellPresentation create(ListView<?> listView) {
      return new WorkCellPresentation(updateService, listView);
    }
  }

  private WorkCellPresentation(StreamCacheUpdateService updateService, ListView<?> listView) {
    this.updateService = updateService;
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
            for(MediaStream stream : work.getStreams()) {
              updateService.reidentifyStream(stream.getId());
            }

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
