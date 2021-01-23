package hs.mediasystem.ui.api.domain;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.ImageHandle;

import java.util.Optional;

public class Parent {
  private final WorkId id;
  private final String name;
  private final Optional<ImageHandle> backdrop;

  public Parent(WorkId id, String name, ImageHandle backdrop) {
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

  public Optional<ImageHandle> getBackdrop() {
    return backdrop;
  }

  public String getName() {
    return name;
  }
}
