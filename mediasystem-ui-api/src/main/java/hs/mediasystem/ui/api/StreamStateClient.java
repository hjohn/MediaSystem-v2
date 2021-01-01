package hs.mediasystem.ui.api;

import hs.mediasystem.domain.stream.ContentID;

import java.time.Instant;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;

public interface StreamStateClient {
  ObjectProperty<Instant> lastWatchedTimeProperty(ContentID contentId);
  ObjectProperty<Integer> resumePositionProperty(ContentID contentId);
  BooleanProperty watchedProperty(ContentID contentId);
}
