package hs.mediasystem.domain.work;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.util.ImageURI;

import java.util.Objects;
import java.util.Optional;

public record Parent(WorkId id, String title, Optional<ImageURI> backdrop) {
  public Parent {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(backdrop, "backdrop");

    if(Objects.requireNonNull(title, "title").isBlank()) {
      throw new IllegalArgumentException("title cannot be blank");
    }
  }

  public MediaType getType() {
    return id.getType();
  }
}
