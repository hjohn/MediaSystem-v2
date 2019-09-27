package hs.mediasystem.runner.grouping;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.plugin.library.scene.MediaItem;

import java.util.List;

public class NoGrouping<T extends MediaDescriptor> implements Grouping<T> {

  @Override
  @SuppressWarnings("unchecked")
  public List<MediaItem<MediaDescriptor>> group(List<MediaItem<T>> items) {
    return (List<MediaItem<MediaDescriptor>>)(List<?>)items;
  }
}
