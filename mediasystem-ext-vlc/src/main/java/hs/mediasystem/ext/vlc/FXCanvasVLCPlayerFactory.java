package hs.mediasystem.ext.vlc;

import hs.ddif.annotations.PluginScoped;
import hs.mediasystem.ext.vlc.VLCPlayer.Mode;

@PluginScoped
public class FXCanvasVLCPlayerFactory extends AbstractVLCPlayerFactory {

  public FXCanvasVLCPlayerFactory() {
    super("VLC (integrated)", Mode.CANVAS, null);
  }

  @Override
  public IntegrationMethod getIntegrationMethod() {
    return IntegrationMethod.PIXEL_BUFFER;
  }
}
