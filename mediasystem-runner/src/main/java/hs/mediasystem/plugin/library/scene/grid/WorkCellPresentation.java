package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.ui.api.StreamStateClient;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.expose.Trigger;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.ListView;

import javax.inject.Inject;
import javax.inject.Singleton;

public class WorkCellPresentation implements Presentation {
  private final WorkClient workClient;
  private final ListView<?> listView;
  private final StreamStateClient streamStateClient;

  @Singleton
  public static class Factory {
    @Inject private WorkClient workClient;
    @Inject private StreamStateClient streamStateClient;

    public WorkCellPresentation create(ListView<?> listView) {
      return new WorkCellPresentation(workClient, streamStateClient, listView);
    }
  }

  private WorkCellPresentation(WorkClient workClient, StreamStateClient streamStateClient, ListView<?> listView) {
    this.workClient = workClient;
    this.streamStateClient = streamStateClient;
    this.listView = listView;
  }

  public BooleanProperty watchedProperty() {
    Object obj = listView.getSelectionModel().getSelectedItem();

    if(obj instanceof Work work && work.getType().isPlayable() && !work.getStreams().isEmpty()) {
      ContentID contentId = work.getPrimaryStream().orElseThrow().getId().getContentId();
      BooleanProperty property = new SimpleBooleanProperty(streamStateClient.isConsumed(contentId));

      property.addListener((ov, old, current) -> {
        streamStateClient.setConsumed(contentId, current);
      });

      return property;
    }

    return null;  // Indicates no state possible as there is no stream
  }

  public Trigger<Void> reidentify() {
    Object obj = listView.getSelectionModel().getSelectedItem();

    if(obj instanceof Work work && !work.getStreams().isEmpty()) {
      return Trigger.asynchronous(event -> {
        workClient.reidentify(work.getId());
      });
    }

    return null;
  }
}
