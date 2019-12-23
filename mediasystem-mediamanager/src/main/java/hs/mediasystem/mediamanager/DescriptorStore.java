package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;

import java.util.Optional;

public interface DescriptorStore {
  Optional<MediaDescriptor> find(Identifier identifier);
}
