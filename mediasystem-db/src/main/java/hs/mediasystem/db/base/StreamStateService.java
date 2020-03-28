package hs.mediasystem.db.base;

import hs.database.util.WeakValueMap;
import hs.mediasystem.domain.stream.ContentID;

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

  public boolean isWatched(ContentID contentId) {
    return streamStateProvider.getOrDefault(contentId, WATCHED_KEY, false);
  }

  public void setWatched(ContentID contentId, boolean watched) {
    watchedProperty(contentId).set(watched);
  }

  public BooleanProperty watchedProperty(ContentID contentId) {
    return (BooleanProperty)properties.computeIfAbsent(contentId.asInt() + "/" + WATCHED_KEY, k -> {
      BooleanProperty property = new SimpleBooleanProperty(isWatched(contentId));

      property.addListener((ov, old, current) -> streamStateProvider.put(contentId, WATCHED_KEY, current));

      return property;
    });
  }

  /**
   * Returns the last time an item was considered watched.
   *
   * @param contentId a {@link ContentID}
   * @return a time, or null if not available
   */
  public Instant getLastWatchedTime(ContentID contentId) {
    String text = streamStateProvider.getOrDefault(contentId, LAST_WATCHED_TIME_KEY, null);

    return text == null ? null : Instant.parse(text.endsWith("Z") ? text : text + "Z");
  }

  public void setLastWatchedTime(ContentID contentId, Instant lastWatchedTime) {
    lastWatchedTimeProperty(contentId).set(lastWatchedTime);
  }

  @SuppressWarnings("unchecked")
  public ObjectProperty<Instant> lastWatchedTimeProperty(ContentID contentId) {
    return (ObjectProperty<Instant>)properties.computeIfAbsent(contentId.asInt() + "/" + LAST_WATCHED_TIME_KEY, k -> {
      ObjectProperty<Instant> property = new SimpleObjectProperty<>(getLastWatchedTime(contentId));

      property.addListener((ov, old, current) -> streamStateProvider.put(contentId, LAST_WATCHED_TIME_KEY, current.toString()));

      return property;
    });
  }

  /**
   * Returns the resume position (in seconds).
   *
   * @param contentId a {@link ContentID}
   * @return the resume position (in seconds), or 0 if there was none
   */
  public int getResumePosition(ContentID contentId) {
    return streamStateProvider.getOrDefault(contentId, RESUME_POSITION_KEY, 0);
  }

  public void setResumePosition(ContentID contentId, int resumePosition) {
    resumePositionProperty(contentId).set(resumePosition);
  }

  @SuppressWarnings("unchecked")
  public ObjectProperty<Integer> resumePositionProperty(ContentID contentId) {
    return (ObjectProperty<Integer>)properties.computeIfAbsent(contentId.asInt() + "/" + RESUME_POSITION_KEY, k -> {
      ObjectProperty<Integer> property = new SimpleObjectProperty<>(getResumePosition(contentId));

      property.addListener((ov, old, current) -> streamStateProvider.put(contentId, RESUME_POSITION_KEY, current));

      return property;
    });
  }

  /**
   * Returns the total duration (in seconds).
   *
   * @param contentId a {@link ContentID}
   * @return the total duration (in seconds), or -1 if unknown
   */
  public int getTotalDuration(ContentID contentId) {
    return streamStateProvider.getOrDefault(contentId, TOTAL_DURATION_KEY, -1);
  }

  public void setTotalDuration(ContentID contentId, int totalDuration) {
    totalDurationProperty(contentId).set(totalDuration);
  }

  @SuppressWarnings("unchecked")
  public ObjectProperty<Integer> totalDurationProperty(ContentID contentId) {
    return (ObjectProperty<Integer>)properties.computeIfAbsent(contentId.asInt() + "/" + TOTAL_DURATION_KEY, k -> {
      ObjectProperty<Integer> property = new SimpleObjectProperty<>(getTotalDuration(contentId));

      property.addListener((ov, old, current) -> streamStateProvider.put(contentId, TOTAL_DURATION_KEY, current));

      return property;
    });
  }

}
