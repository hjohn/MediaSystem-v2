package hs.mediasystem.ext.vlc;

import hs.mediasystem.ext.vlc.VLCPlayer.Mode;

import javax.inject.Singleton;

@Singleton
public class FXCanvasVLCPlayerFactory extends AbstractVLCPlayerFactory {

  public FXCanvasVLCPlayerFactory() {
    super("VLC (integrated)", Mode.CANVAS, null);
  }
}
