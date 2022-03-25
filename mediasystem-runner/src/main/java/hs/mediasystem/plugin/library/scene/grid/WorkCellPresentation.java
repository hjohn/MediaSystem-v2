package hs.mediasystem.plugin.library.scene.grid;

import hs.ddif.annotations.Argument;
import hs.ddif.annotations.Assisted;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.EventRoot;
import hs.mediasystem.ui.api.ConsumedStateChanged;
import hs.mediasystem.ui.api.StreamStateClient;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.expose.Trigger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;

import javax.inject.Inject;

@Assisted
public class WorkCellPresentation implements Presentation {
  @Inject private WorkClient workClient;
  @Inject private StreamStateClient streamStateClient;
  @Inject private EventRoot eventRoot;
  @Inject @Argument private ObservableValue<?> selectedItem;

  public BooleanProperty watchedProperty() {
    Object obj = selectedItem.getValue();

    if(obj instanceof Work work && work.getType().isPlayable() && !work.getStreams().isEmpty()) {
      ContentID contentId = work.getPrimaryStream().orElseThrow().getId().getContentId();
      BooleanProperty property = new SimpleBooleanProperty(streamStateClient.isConsumed(contentId));

      property.addListener((ov, old, current) -> {
        streamStateClient.setConsumed(contentId, current);
        eventRoot.fire(new ConsumedStateChanged(contentId, current));
      });

      return property;
    }

    return null;  // Indicates no state possible as there is no stream
  }

  public Trigger<Void> reidentify() {
    Object obj = selectedItem.getValue();

    if(obj instanceof Work work && !work.getStreams().isEmpty()) {
      return Trigger.asynchronous(event -> {
        workClient.reidentify(work.getId());
      });
    }

    return null;
  }
}
