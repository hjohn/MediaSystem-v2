package hs.mediasystem.ui.api.domain;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.image.ImageHandle;

import java.util.Optional;

public record Context(WorkId id, String title, Optional<ImageHandle> backdrop) {
  public MediaType type() {
    return id.getType();
  }
}
