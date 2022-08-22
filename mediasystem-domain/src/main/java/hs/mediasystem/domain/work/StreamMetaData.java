package hs.mediasystem.domain.work;

import com.fasterxml.jackson.annotation.JsonAlias;

import hs.mediasystem.domain.stream.ContentID;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StreamMetaData {
  private final ContentID contentId;
  private final Optional<Duration> duration;
  @JsonAlias("videoStreams") private final List<VideoTrack> videoTracks;
  private final List<AudioTrack> audioTracks;
  private final List<SubtitleTrack> subtitleTracks;
  private final List<Snapshot> snapshots;

  // TODO remove aliases after a while -- think of a way to migrate; might be good to store JSON not as byte[]
  public StreamMetaData(ContentID contentId, Duration duration, @JsonAlias("videoStreams") List<VideoTrack> videoTracks, @JsonAlias("audioStreams") List<AudioTrack> audioTracks, @JsonAlias("subtitleStreams") List<SubtitleTrack> subtitleTracks, List<Snapshot> snapshots) {
    if(contentId == null) {
      throw new IllegalArgumentException("contentId cannot be null");
    }
    if(videoTracks == null) {
      throw new IllegalArgumentException("videoTracks cannot be null");
    }
    if(audioTracks == null) {
      throw new IllegalArgumentException("audioTracks cannot be null");
    }
    if(subtitleTracks == null) {
      throw new IllegalArgumentException("subtitleTracks cannot be null");
    }
    if(snapshots == null) {
      throw new IllegalArgumentException("snapshots cannot be null");
    }
    if(snapshots.stream().filter(Objects::isNull).findAny().isPresent()) {
      throw new IllegalArgumentException("snapshots cannot contain nulls");
    }

    this.contentId = contentId;
    this.duration = Optional.ofNullable(duration);
    this.videoTracks = Collections.unmodifiableList(videoTracks);
    this.audioTracks = Collections.unmodifiableList(audioTracks);
    this.subtitleTracks = Collections.unmodifiableList(subtitleTracks);
    this.snapshots = Collections.unmodifiableList(snapshots);
  }

  public ContentID getContentId() {
    return contentId;
  }

  /**
   * Returns the duration discovered as part of this metadata.  This
   * is optional as not all streams have a duration (a directory for a example).
   *
   * @return the duration, optional
   */
  public Optional<Duration> getLength() {
    return duration;
  }

  public List<VideoTrack> getVideoTracks() {
    return videoTracks;
  }

  public List<AudioTrack> getAudioTracks() {
    return audioTracks;
  }

  public List<SubtitleTrack> getSubtitleTracks() {
    return subtitleTracks;
  }

  public List<Snapshot> getSnapshots() {
    return snapshots;
  }
}
