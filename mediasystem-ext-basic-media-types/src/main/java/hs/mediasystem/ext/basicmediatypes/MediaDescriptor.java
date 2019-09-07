package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.ext.basicmediatypes.domain.Details;

public interface MediaDescriptor {
  Identifier getIdentifier();
  Details getDetails();
}
