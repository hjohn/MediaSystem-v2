package hs.mediasystem.ext.mpv;

import hs.mediasystem.domain.PlayerFactory;
import hs.mediasystem.domain.PlayerPresentation;
import hs.mediasystem.domain.PlayerWindowIdSupplier;
import hs.mediasystem.util.ini.Ini;

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
  public PlayerPresentation create(Ini ini) {
    return new MPVPlayer(supplier);
  }
}
