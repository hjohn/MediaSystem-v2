package hs.mediasystem.domain.work;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.util.image.ImageURI;

import java.util.Objects;
import java.util.Optional;

public record Context(WorkId id, String title, Optional<ImageURI> cover, Optional<ImageURI> backdrop) {
  public Context {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(cover, "cover");
    Objects.requireNonNull(backdrop, "backdrop");

    if(Objects.requireNonNull(title, "title").isBlank()) {
      throw new IllegalArgumentException("title cannot be blank");
    }
  }

  public MediaType getType() {
    return id.getType();
  }
}
