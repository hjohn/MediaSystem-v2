package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;

import java.util.Optional;

public interface DescriptorStore {
  Optional<WorkDescriptor> find(WorkId id);
}
