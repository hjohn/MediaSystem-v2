package hs.mediasystem.ext.vlc;

import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapter;

/**
 * Small hack to make it possible to supply VLCJ v3 with a window id directly.  When
 * upgraded to VLCJ v4 this can be removed.
 */
public abstract class DeferredComponentIdVideoSurface extends VideoSurface {

  /**
   * Create a new video surface.
   *
   * @param videoSurfaceAdapter adapter to attach a video surface to a native media player
   */
  public DeferredComponentIdVideoSurface(VideoSurfaceAdapter videoSurfaceAdapter) {
    super(videoSurfaceAdapter);
  }

  @Override
  public void attach(MediaPlayer mediaPlayer) {
    videoSurfaceAdapter.attach(mediaPlayer, getComponentId());
  }

  /**
   * Get the native component id to use for the video surface.
   *
   * @return component id
   */
  protected abstract long getComponentId();

}