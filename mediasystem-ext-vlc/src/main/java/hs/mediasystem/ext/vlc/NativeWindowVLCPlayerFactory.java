package hs.mediasystem.ext.vlc;

import hs.mediasystem.domain.PlayerWindowIdSupplier;
import hs.mediasystem.ext.vlc.VLCPlayer.Mode;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NativeWindowVLCPlayerFactory extends AbstractVLCPlayerFactory {

  @Inject
  public NativeWindowVLCPlayerFactory(PlayerWindowIdSupplier supplier) {
    super("VLC (native window)", Mode.WID, supplier);
  }
}
