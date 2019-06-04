package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;

public interface DetailedMediaDescriptor extends MediaDescriptor {
  Details getDetails();
}
