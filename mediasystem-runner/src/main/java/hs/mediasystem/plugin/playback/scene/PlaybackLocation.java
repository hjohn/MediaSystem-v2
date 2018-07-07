package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.plugin.library.scene.MediaItem;

import java.net.URI;

public class PlaybackLocation {
  private final MediaItem<?> mediaItem;
  private final URI uri;

  public PlaybackLocation(MediaItem<?> mediaItem, URI uri) {
    if(mediaItem == null) {
      throw new IllegalArgumentException("mediaItem cannot be null");
    }
    if(uri == null) {
      throw new IllegalArgumentException("uri cannot be null");
    }

    this.mediaItem = mediaItem;
    this.uri = uri;
  }

  public MediaItem<?> getMediaItem() {
    return mediaItem;
  }

  public URI getUri() {
    return uri;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[uri=" + uri + "; " + mediaItem + "]";
  }
}
