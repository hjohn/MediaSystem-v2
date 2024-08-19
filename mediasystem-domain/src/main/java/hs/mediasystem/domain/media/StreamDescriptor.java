package hs.mediasystem.domain.media;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record StreamDescriptor(
  Optional<Duration> duration,
  List<VideoTrack> videoTracks,
  List<AudioTrack> audioTracks,
  List<SubtitleTrack> subtitleTracks,
  List<Snapshot> snapshots
) {
  public StreamDescriptor {
    if(duration == null) {
      throw new IllegalArgumentException("duration cannot be null");
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
  }
}
