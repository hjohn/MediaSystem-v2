package hs.mediasystem.ui.api;

import hs.mediasystem.domain.stream.ContentID;

import java.time.Duration;
import java.time.Instant;

public interface StreamStateClient {
  boolean isConsumed(ContentID contentId);
  void setConsumed(ContentID contentId, boolean consumed);

  Instant getLastConsumptionTime(ContentID contentId);
  void setLastConsumptionTime(ContentID contentId, Instant time);

  Duration getResumePosition(ContentID contentId);
  void setResumePosition(ContentID contentId, Duration duration);
}
