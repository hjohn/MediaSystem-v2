package hs.mediasystem.domain.work;

import hs.mediasystem.domain.stream.StreamID;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class StreamMetaData {
  private final StreamID streamId;
  private final Duration duration;
  private final List<VideoStream> videoStreams;
  private final List<AudioStream> audioStreams;
  private final List<SubtitleStream> subtitleStreams;
  private final List<Snapshot> snapshots;

  public StreamMetaData(StreamID streamId, Duration duration, List<VideoStream> videoStreams, List<AudioStream> audioStreams, List<SubtitleStream> subtitleStreams, List<Snapshot> snapshots) {
    if(streamId == null) {
      throw new IllegalArgumentException("streamId cannot be null");
    }
    if(videoStreams == null) {
      throw new IllegalArgumentException("videoStreams cannot be null");
    }
    if(audioStreams == null) {
      throw new IllegalArgumentException("audioStreams cannot be null");
    }
    if(subtitleStreams == null) {
      throw new IllegalArgumentException("subtitleStreams cannot be null");
    }
    if(duration == null) {
      throw new IllegalArgumentException("duration cannot be null");
    }
    if(snapshots == null) {
      throw new IllegalArgumentException("snapshots cannot be null");
    }
    if(snapshots.stream().filter(Objects::isNull).findAny().isPresent()) {
      throw new IllegalArgumentException("snapshots cannot contain nulls");
    }

    this.streamId = streamId;
    this.duration = duration;
    this.videoStreams = Collections.unmodifiableList(videoStreams);
    this.audioStreams = Collections.unmodifiableList(audioStreams);
    this.subtitleStreams = Collections.unmodifiableList(subtitleStreams);
    this.snapshots = Collections.unmodifiableList(snapshots);
  }

  public StreamID getStreamId() {
    return streamId;
  }

  public Duration getLength() {
    return duration;
  }

  public List<VideoStream> getVideoStreams() {
    return videoStreams;
  }

  public List<AudioStream> getAudioStreams() {
    return audioStreams;
  }

  public List<SubtitleStream> getSubtitleStreams() {
    return subtitleStreams;
  }

  public List<Snapshot> getSnapshots() {
    return snapshots;
  }
}
