package hs.mediasystem.db;

import hs.database.util.WeakValueMap;
import hs.mediasystem.ext.basicmediatypes.scan.StreamPrint;

import java.util.Map;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamStateService {
  private static final String WATCHED_KEY = "watched";
  private static final String RESUME_POSITION_KEY = "resume-position";

  @Inject private StreamStateProvider streamStateProvider;

  private final Map<String, ObjectProperty<?>> properties = new WeakValueMap<>();

  public boolean isWatched(StreamPrint streamPrint) {
    return streamStateProvider.getOrDefault(streamPrint, WATCHED_KEY, false);
  }

  public void setWatched(StreamPrint streamPrint, boolean watched) {
    streamStateProvider.put(streamPrint, WATCHED_KEY, watched);
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
    streamStateProvider.put(streamPrint, RESUME_POSITION_KEY, resumePosition);

    @SuppressWarnings("unchecked")
    ObjectProperty<Integer> p = (ObjectProperty<Integer>)properties.get(streamPrint.getIdentifier() + "/" + RESUME_POSITION_KEY);

    if(p != null) {
      p.set(resumePosition);
    }
  }

  public ObjectProperty<Integer> resumePositionProperty(StreamPrint streamPrint) {
    @SuppressWarnings("unchecked")
    ObjectProperty<Integer> p = (ObjectProperty<Integer>)properties.computeIfAbsent(streamPrint.getIdentifier() + "/" + RESUME_POSITION_KEY, k -> new SimpleObjectProperty<>(getResumePosition(streamPrint)));

    p.addListener((ov, old, current) -> setResumePosition(streamPrint, current));

    return p;
  }
}
