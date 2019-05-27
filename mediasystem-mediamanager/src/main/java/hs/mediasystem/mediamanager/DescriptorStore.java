package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;

public interface DescriptorStore {
  MediaDescriptor get(Identifier identifier);
}
