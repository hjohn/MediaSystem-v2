package hs.mediasystem.local.client.service;

import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.ui.api.StreamStateClient;

import java.time.Instant;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalStreamStateClient implements StreamStateClient {
  @Inject private StreamStateService service;

  @Override
  public ObjectProperty<Instant> lastWatchedTimeProperty(ContentID contentId) {
    return service.lastWatchedTimeProperty(contentId);
  }

  @Override
  public ObjectProperty<Integer> resumePositionProperty(ContentID contentId) {
    return service.resumePositionProperty(contentId);
  }

  @Override
  public BooleanProperty watchedProperty(ContentID contentId) {
    return service.watchedProperty(contentId);
  }
}
