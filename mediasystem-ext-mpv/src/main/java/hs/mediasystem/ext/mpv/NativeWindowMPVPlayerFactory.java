package hs.mediasystem.ext.mpv;

import hs.mediasystem.ui.api.player.PlayerFactory;
import hs.mediasystem.ui.api.player.PlayerPresentation;
import hs.mediasystem.ui.api.player.PlayerWindowIdSupplier;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NativeWindowMPVPlayerFactory implements PlayerFactory {
  @Inject private PlayerWindowIdSupplier supplier;

  @Override
  public String getName() {
    return "MPV";
  }

  @Override
  public PlayerPresentation create() {
    return new MPVPlayer(supplier);
  }

  @Override
  public IntegrationMethod getIntegrationMethod() {
    return IntegrationMethod.WINDOW;
  }
}
