package hs.mediasystem.domain.work;

import java.util.List;

public record MediaStructure(List<VideoTrack> videoTracks, List<AudioTrack> audioTracks, List<SubtitleTrack> subtitleTracks) {
  public MediaStructure {
    if(videoTracks == null) {
      throw new IllegalArgumentException("videoTracks cannot be null");
    }
    if(audioTracks == null) {
      throw new IllegalArgumentException("audioTracks cannot be null");
    }
    if(subtitleTracks == null) {
      throw new IllegalArgumentException("subtitleTracks cannot be null");
    }
  }
}
