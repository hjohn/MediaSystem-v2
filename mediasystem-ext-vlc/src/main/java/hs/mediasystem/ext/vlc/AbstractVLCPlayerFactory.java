package hs.mediasystem.ext.vlc;

import com.sun.jna.NativeLibrary;

import hs.mediasystem.domain.PlayerWindowIdSupplier;
import hs.mediasystem.domain.PlayerFactory;
import hs.mediasystem.domain.PlayerPresentation;
import hs.mediasystem.ext.vlc.VLCPlayer.Mode;
import hs.mediasystem.util.ini.Ini;
import hs.mediasystem.util.ini.Section;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
  public PlayerPresentation create(Ini ini) {
    Path libVlcPath;

    if(System.getProperty("os.arch").equals("x86")) {
      libVlcPath = Paths.get("c:/program files (x86)/VideoLAN/VLC");
    }
    else {
      libVlcPath = Paths.get("c:/program files/VideoLAN/VLC");
    }

    NativeLibrary.addSearchPath("libvlc", libVlcPath.toString());

    List<String> args = new ArrayList<>();
    Section vlcArgsSection = ini.getSection("vlc.args");

    if(vlcArgsSection != null) {
      for(String key : vlcArgsSection) {
        args.add(key);
        args.add(vlcArgsSection.get(key));
      }
    }

    return new VLCPlayer(mode, supplier, args.toArray(new String[args.size()]));
  }

  @Override
  public String getName() {
    return name;
  }
}
