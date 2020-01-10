package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;

public interface MediaDescriptor {
  Identifier getIdentifier();
  Details getDetails();
}
