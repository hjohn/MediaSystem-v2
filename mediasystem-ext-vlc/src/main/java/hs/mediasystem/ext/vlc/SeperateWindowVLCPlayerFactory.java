package hs.mediasystem.ext.vlc;

import hs.mediasystem.ext.vlc.VLCPlayer.Mode;

import javax.inject.Singleton;

@Singleton
public class SeperateWindowVLCPlayerFactory extends AbstractVLCPlayerFactory {

  public SeperateWindowVLCPlayerFactory() {
    super("VLC (seperate window)", Mode.SEPERATE_WINDOW);
  }
}
