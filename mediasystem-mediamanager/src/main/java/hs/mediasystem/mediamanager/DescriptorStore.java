package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;

import java.util.Optional;

public interface DescriptorStore {
  Optional<MediaDescriptor> find(Identifier identifier);
}
