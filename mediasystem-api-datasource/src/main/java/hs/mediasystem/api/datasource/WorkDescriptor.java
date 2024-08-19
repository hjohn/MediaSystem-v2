package hs.mediasystem.api.datasource;

import hs.mediasystem.api.datasource.domain.Details;
import hs.mediasystem.domain.work.WorkId;

/**
 * Describes a work (as in a work of art).
 */
public interface WorkDescriptor {  // TODO rename this one to Work?
  WorkId getId();
  Details getDetails();

  default WorkId id() {
    return getId();
  }
}
