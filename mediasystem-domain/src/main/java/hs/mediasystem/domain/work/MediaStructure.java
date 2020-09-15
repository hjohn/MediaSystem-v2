package hs.mediasystem.domain.work;

import java.util.List;

public class MediaStructure {
  private final List<VideoTrack> videoTracks;
  private final List<AudioTrack> audioTracks;
  private final List<SubtitleTrack> subtitleTracks;

  public MediaStructure(List<VideoTrack> videoTracks, List<AudioTrack> audioTracks, List<SubtitleTrack> subtitleTracks) {
    if(videoTracks == null) {
      throw new IllegalArgumentException("videoTracks cannot be null");
    }
    if(audioTracks == null) {
      throw new IllegalArgumentException("audioTracks cannot be null");
    }
    if(subtitleTracks == null) {
      throw new IllegalArgumentException("subtitleTracks cannot be null");
    }

    this.videoTracks = List.copyOf(videoTracks);
    this.audioTracks = List.copyOf(audioTracks);
    this.subtitleTracks = List.copyOf(subtitleTracks);
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
}
