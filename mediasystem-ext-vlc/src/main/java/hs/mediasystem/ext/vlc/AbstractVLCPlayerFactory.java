package hs.mediasystem.ext.vlc;

import com.sun.jna.NativeLibrary;

import hs.mediasystem.ext.vlc.VLCPlayer.Mode;
import hs.mediasystem.ui.api.player.PlayerFactory;
import hs.mediasystem.ui.api.player.PlayerPresentation;
import hs.mediasystem.ui.api.player.PlayerWindowIdSupplier;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractVLCPlayerFactory implements PlayerFactory {
  private final String name;
  private final Mode mode;
  private final PlayerWindowIdSupplier supplier;

  public AbstractVLCPlayerFactory(String name, Mode mode, PlayerWindowIdSupplier supplier) {
    this.name = name;
    this.mode = mode;
    this.supplier = supplier;
  }

  @Override
  public PlayerPresentation create() {
    Path libVlcPath;

    if(System.getProperty("os.arch").equals("x86")) {
      libVlcPath = Paths.get("c:/program files (x86)/VideoLAN/VLC");
    }
    else {
      libVlcPath = Paths.get("c:/program files/VideoLAN/VLC");
    }

    NativeLibrary.addSearchPath("libvlc", libVlcPath.toString());

    return new VLCPlayer(mode, supplier);
  }

  @Override
  public String getName() {
    return name;
  }
}
