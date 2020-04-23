package hs.mediasystem.domain.work;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.util.ImageURI;

import java.util.Optional;

public class Parent {
  private final WorkId id;
  private final String name;
  private final Optional<ImageURI> backdrop;

  public Parent(WorkId id, String name, ImageURI backdrop) {
    this.id = id;
    this.name = name;
    this.backdrop = Optional.ofNullable(backdrop);
  }

  public WorkId getId() {
    return id;
  }

  public MediaType getType() {
    return id.getType();
  }

  public Optional<ImageURI> getBackdrop() {
    return backdrop;
  }

  public String getName() {
    return name;
  }
}
