package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.domain.Details;

/**
 * Describes a work (as in a work of art).
 */
public interface WorkDescriptor {
  WorkId getId();
  Details getDetails();
}
