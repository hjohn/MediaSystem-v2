package hs.mediasystem.local.client.service;

import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.ui.api.StreamStateClient;

import java.time.Duration;
import java.time.Instant;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalStreamStateClient implements StreamStateClient {
  @Inject private StreamStateService service;

  @Override
  public boolean isConsumed(ContentID contentId) {
    return service.isWatched(contentId);
  }

  @Override
  public void setConsumed(ContentID contentId, boolean consumed) {
    service.setWatched(contentId, consumed);
  }

  @Override
  public Instant getLastConsumptionTime(ContentID contentId) {
    return service.getLastWatchedTime(contentId);
  }

  @Override
  public void setLastConsumptionTime(ContentID contentId, Instant time) {
    service.setLastWatchedTime(contentId, time);
  }

  @Override
  public Duration getResumePosition(ContentID contentId) {
    return Duration.ofSeconds(service.getResumePosition(contentId));
  }

  @Override
  public void setResumePosition(ContentID contentId, Duration duration) {
    service.setResumePosition(contentId, (int)duration.toSeconds());
  }
}
