package hs.mediasystem.domain.work;

import com.fasterxml.jackson.annotation.JsonAlias;

import hs.mediasystem.domain.stream.ContentID;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record StreamMetaData(
  ContentID contentId,
  @JsonAlias("duration") Optional<Duration> length,
  @JsonAlias("videoStreams") List<VideoTrack> videoTracks,
  @JsonAlias("audioStreams") List<AudioTrack> audioTracks,
  @JsonAlias("subtitleStreams") List<SubtitleTrack> subtitleTracks,
  List<Snapshot> snapshots) {

  // TODO remove aliases after a while -- think of a way to migrate; might be good to store JSON not as byte[]
  public StreamMetaData {
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
  }
}
