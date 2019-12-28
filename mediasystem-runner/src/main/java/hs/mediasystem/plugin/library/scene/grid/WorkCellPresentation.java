package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.db.StreamCacheUpdateService;
import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.domain.stream.MediaStream;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.presentation.Presentation;

import javafx.beans.property.BooleanProperty;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.control.ListView;

import javax.inject.Inject;
import javax.inject.Singleton;

public class WorkCellPresentation implements Presentation {
  private final StreamCacheUpdateService updateService;
  private final ListView<?> listView;
  private final StreamStateService streamStateService;

  @Singleton
  public static class Factory {
    @Inject private StreamStateService streamStateService;
    @Inject private StreamCacheUpdateService updateService;

    public WorkCellPresentation create(ListView<?> listView) {
      return new WorkCellPresentation(updateService, streamStateService, listView);
    }
  }

  private WorkCellPresentation(StreamCacheUpdateService updateService, StreamStateService streamStateService, ListView<?> listView) {
    this.updateService = updateService;
    this.streamStateService = streamStateService;
    this.listView = listView;
  }

  public BooleanProperty watchedProperty() {
    Object obj = listView.getSelectionModel().getSelectedItem();

    if(obj instanceof Work) {
      Work work = (Work)obj;

      return work.getPrimaryStream()
        .map(MediaStream::getId)
        .map(streamStateService::watchedProperty)
        .orElse(null);
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
