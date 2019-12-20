package hs.mediasystem.db;

import hs.database.util.WeakValueMap;
import hs.mediasystem.scanner.api.StreamID;

import java.time.Instant;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamStateService {
  private static final String WATCHED_KEY = "watched";
  private static final String LAST_WATCHED_TIME_KEY = "last-watched-time";
  private static final String RESUME_POSITION_KEY = "resume-position";
  private static final String TOTAL_DURATION_KEY = "total-duration";

  @Inject private StreamStateProvider streamStateProvider;

  private final Map<String, Property<?>> properties = new WeakValueMap<>();

  public boolean isWatched(StreamID streamId) {
    return streamStateProvider.getOrDefault(streamId, WATCHED_KEY, false);
  }

  public void setWatched(StreamID streamId, boolean watched) {
    watchedProperty(streamId).set(watched);
  }

  public BooleanProperty watchedProperty(StreamID streamId) {
    return (BooleanProperty)properties.computeIfAbsent(streamId.asInt() + "/" + WATCHED_KEY, k -> {
      BooleanProperty property = new SimpleBooleanProperty(isWatched(streamId));

      property.addListener((ov, old, current) -> streamStateProvider.put(streamId, WATCHED_KEY, current));

      return property;
    });
  }

  /**
   * Returns the last time an item was considered watched.
   *
   * @param streamId a {@link StreamID}
   * @return a time, or null if not available
   */
  public Instant getLastWatchedTime(StreamID streamId) {
    String text = streamStateProvider.getOrDefault(streamId, LAST_WATCHED_TIME_KEY, null);

    return text == null ? null : Instant.parse(text.endsWith("Z") ? text : text + "Z");
  }

  public void setLastWatchedTime(StreamID streamId, Instant lastWatchedTime) {
    lastWatchedTimeProperty(streamId).set(lastWatchedTime);
  }

  @SuppressWarnings("unchecked")
  public ObjectProperty<Instant> lastWatchedTimeProperty(StreamID streamId) {
    return (ObjectProperty<Instant>)properties.computeIfAbsent(streamId.asInt() + "/" + LAST_WATCHED_TIME_KEY, k -> {
      ObjectProperty<Instant> property = new SimpleObjectProperty<>(getLastWatchedTime(streamId));

      property.addListener((ov, old, current) -> streamStateProvider.put(streamId, LAST_WATCHED_TIME_KEY, current.toString()));

      return property;
    });
  }

  /**
   * Returns the resume position (in seconds).
   *
   * @param streamId a {@link StreamID}
   * @return the resume position (in seconds), or 0 if there was none
   */
  public int getResumePosition(StreamID streamId) {
    return streamStateProvider.getOrDefault(streamId, RESUME_POSITION_KEY, 0);
  }

  public void setResumePosition(StreamID streamId, int resumePosition) {
    resumePositionProperty(streamId).set(resumePosition);
  }

  @SuppressWarnings("unchecked")
  public ObjectProperty<Integer> resumePositionProperty(StreamID streamId) {
    return (ObjectProperty<Integer>)properties.computeIfAbsent(streamId.asInt() + "/" + RESUME_POSITION_KEY, k -> {
      ObjectProperty<Integer> property = new SimpleObjectProperty<>(getResumePosition(streamId));

      property.addListener((ov, old, current) -> streamStateProvider.put(streamId, RESUME_POSITION_KEY, current));

      return property;
    });
  }

  /**
   * Returns the total duration (in seconds).
   *
   * @param streamId a {@link StreamID}
   * @return the total duration (in seconds), or -1 if unknown
   */
  public int getTotalDuration(StreamID streamId) {
    return streamStateProvider.getOrDefault(streamId, TOTAL_DURATION_KEY, -1);
  }

  public void setTotalDuration(StreamID streamId, int totalDuration) {
    totalDurationProperty(streamId).set(totalDuration);
  }

  @SuppressWarnings("unchecked")
  public ObjectProperty<Integer> totalDurationProperty(StreamID streamId) {
    return (ObjectProperty<Integer>)properties.computeIfAbsent(streamId.asInt() + "/" + TOTAL_DURATION_KEY, k -> {
      ObjectProperty<Integer> property = new SimpleObjectProperty<>(getTotalDuration(streamId));

      property.addListener((ov, old, current) -> streamStateProvider.put(streamId, TOTAL_DURATION_KEY, current));

      return property;
    });
  }

}
