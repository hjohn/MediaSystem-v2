package hs.mediasystem.db;

import hs.database.util.WeakValueMap;
import hs.mediasystem.scanner.api.StreamPrint;

import java.time.LocalDateTime;
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

  public boolean isWatched(StreamPrint streamPrint) {
    return streamStateProvider.getOrDefault(streamPrint, WATCHED_KEY, false);
  }

  public void setWatched(StreamPrint streamPrint, boolean watched) {
    watchedProperty(streamPrint).set(watched);
  }

  public BooleanProperty watchedProperty(StreamPrint streamPrint) {
    return (BooleanProperty)properties.computeIfAbsent(streamPrint.getId().asInt() + "/" + WATCHED_KEY, k -> {
      BooleanProperty property = new SimpleBooleanProperty(isWatched(streamPrint));

      property.addListener((ov, old, current) -> streamStateProvider.put(streamPrint, WATCHED_KEY, current));

      return property;
    });
  }

  /**
   * Returns the last time an item was considered watched.
   *
   * @param streamPrint a {@link StreamPrint}
   * @return a time, or null if not available
   */
  public LocalDateTime getLastWatchedTime(StreamPrint streamPrint) {
    String text = streamStateProvider.getOrDefault(streamPrint, LAST_WATCHED_TIME_KEY, null);

    return text == null ? null : LocalDateTime.parse(text);
  }

  public void setLastWatchedTime(StreamPrint streamPrint, LocalDateTime lastWatchedTime) {
    lastWatchedTimeProperty(streamPrint).set(lastWatchedTime);
  }

  @SuppressWarnings("unchecked")
  public ObjectProperty<LocalDateTime> lastWatchedTimeProperty(StreamPrint streamPrint) {
    return (ObjectProperty<LocalDateTime>)properties.computeIfAbsent(streamPrint.getId().asInt() + "/" + LAST_WATCHED_TIME_KEY, k -> {
      ObjectProperty<LocalDateTime> property = new SimpleObjectProperty<>(getLastWatchedTime(streamPrint));

      property.addListener((ov, old, current) -> streamStateProvider.put(streamPrint, LAST_WATCHED_TIME_KEY, current.toString()));

      return property;
    });
  }

  /**
   * Returns the resume position (in seconds).
   *
   * @param streamPrint a {@link StreamPrint}
   * @return the resume position (in seconds), or 0 if there was none
   */
  public int getResumePosition(StreamPrint streamPrint) {
    return streamStateProvider.getOrDefault(streamPrint, RESUME_POSITION_KEY, 0);
  }

  public void setResumePosition(StreamPrint streamPrint, int resumePosition) {
    resumePositionProperty(streamPrint).set(resumePosition);
  }

  @SuppressWarnings("unchecked")
  public ObjectProperty<Integer> resumePositionProperty(StreamPrint streamPrint) {
    return (ObjectProperty<Integer>)properties.computeIfAbsent(streamPrint.getId().asInt() + "/" + RESUME_POSITION_KEY, k -> {
      ObjectProperty<Integer> property = new SimpleObjectProperty<>(getResumePosition(streamPrint));

      property.addListener((ov, old, current) -> streamStateProvider.put(streamPrint, RESUME_POSITION_KEY, current));

      return property;
    });
  }

  /**
   * Returns the total duration (in seconds).
   *
   * @param streamPrint a {@link StreamPrint}
   * @return the total duration (in seconds), or -1 if unknown
   */
  public int getTotalDuration(StreamPrint streamPrint) {
    return streamStateProvider.getOrDefault(streamPrint, TOTAL_DURATION_KEY, -1);
  }

  public void setTotalDuration(StreamPrint streamPrint, int totalDuration) {
    totalDurationProperty(streamPrint).set(totalDuration);
  }

  @SuppressWarnings("unchecked")
  public ObjectProperty<Integer> totalDurationProperty(StreamPrint streamPrint) {
    return (ObjectProperty<Integer>)properties.computeIfAbsent(streamPrint.getId().asInt() + "/" + TOTAL_DURATION_KEY, k -> {
      ObjectProperty<Integer> property = new SimpleObjectProperty<>(getTotalDuration(streamPrint));

      property.addListener((ov, old, current) -> streamStateProvider.put(streamPrint, TOTAL_DURATION_KEY, current));

      return property;
    });
  }

}
