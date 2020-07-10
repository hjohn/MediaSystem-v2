package hs.mediasystem.ext.vlc;

import hs.ddif.annotations.PluginScoped;
import hs.mediasystem.ext.vlc.VLCPlayer.Mode;
import hs.mediasystem.ui.api.player.PlayerWindowIdSupplier;

import javax.inject.Inject;

@PluginScoped
public class NativeWindowVLCPlayerFactory extends AbstractVLCPlayerFactory {

  @Inject
  public NativeWindowVLCPlayerFactory(PlayerWindowIdSupplier supplier) {
    super("VLC (native window)", Mode.WID, supplier);
  }

  @Override
  public IntegrationMethod getIntegrationMethod() {
    return IntegrationMethod.WINDOW;
  }
}
