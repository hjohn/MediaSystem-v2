package hs.mediasystem.runner.grouping;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.plugin.library.scene.MediaItem;

import java.util.List;

public interface Grouping<I extends MediaDescriptor> {
  List<MediaItem<MediaDescriptor>> group(List<MediaItem<I>> items);
}
