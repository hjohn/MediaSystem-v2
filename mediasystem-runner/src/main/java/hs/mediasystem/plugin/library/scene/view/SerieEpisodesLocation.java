package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.plugin.library.scene.LibraryLocation;
import hs.mediasystem.plugin.library.scene.MediaItem;

public class SerieEpisodesLocation extends LibraryLocation {

  public SerieEpisodesLocation(MediaItem<?> item, int number) {
    super(item, "Season:" + number);
  }

}
