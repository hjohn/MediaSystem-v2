package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;

public interface DescriptorStore {
  void add(MediaDescriptor descriptor);
  MediaDescriptor get(Identifier identifier);
}
