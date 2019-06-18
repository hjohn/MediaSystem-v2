package hs.mediasystem.ext.vlc;

import java.awt.Canvas;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapter;

/**
 * Small hack to make it possible to supply VLCJ v3 with a window id directly.  When
 * upgraded to VLCJ v4 this can be removed.
 */
public abstract class DeferredComponentIdVideoSurface extends CanvasVideoSurface {

  private static final Canvas canvas = new Canvas() {
    @Override
    public boolean isVisible() {
      return true;  // VLCJ checks visibility before calling attach, not needed when using wid.
    }
  };

  /**
   * Create a new video surface.
   *
   * @param videoSurfaceAdapter adapter to attach a video surface to a native media player
   */
  public DeferredComponentIdVideoSurface(VideoSurfaceAdapter videoSurfaceAdapter) {
    super(canvas, videoSurfaceAdapter);
  }

  @Override
  public void attach(LibVlc libvlc, MediaPlayer mediaPlayer) {
    videoSurfaceAdapter.attach(libvlc, mediaPlayer, getComponentId());
  }

  /**
   * Get the native component id to use for the video surface.
   *
   * @return component id
   */
  protected abstract long getComponentId();

}