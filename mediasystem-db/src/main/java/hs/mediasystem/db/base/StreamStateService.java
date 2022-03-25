package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.ContentID;

import java.time.Instant;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamStateService {
  private static final String WATCHED_KEY = "watched";
  private static final String LAST_WATCHED_TIME_KEY = "last-watched-time";
  private static final String RESUME_POSITION_KEY = "resume-position";
  private static final String TOTAL_DURATION_KEY = "total-duration";

  @Inject private StreamStateProvider streamStateProvider;

  public boolean isWatched(ContentID contentId) {
    return streamStateProvider.getOrDefault(contentId, WATCHED_KEY, false);
  }

  public void setWatched(ContentID contentId, boolean watched) {
    streamStateProvider.put(contentId, WATCHED_KEY, watched);
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
    streamStateProvider.put(contentId, LAST_WATCHED_TIME_KEY, lastWatchedTime.toString());
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
    streamStateProvider.put(contentId, RESUME_POSITION_KEY, resumePosition);
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
    streamStateProvider.put(contentId, TOTAL_DURATION_KEY, totalDuration);
  }
}
