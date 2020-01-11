package hs.mediasystem.ext.vlc;

import hs.mediasystem.ext.vlc.VLCPlayer.Mode;
import hs.mediasystem.ui.api.player.PlayerWindowIdSupplier;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NativeWindowVLCPlayerFactory extends AbstractVLCPlayerFactory {

  @Inject
  public NativeWindowVLCPlayerFactory(PlayerWindowIdSupplier supplier) {
    super("VLC (native window)", Mode.WID, supplier);
  }
}
