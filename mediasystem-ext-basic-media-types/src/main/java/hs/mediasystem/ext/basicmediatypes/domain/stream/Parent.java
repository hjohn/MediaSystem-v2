package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.scanner.api.MediaType;

public class Parent {
  private final WorkId id;
  private final MediaType type;
  private final String name;

  public Parent(WorkId id, MediaType type, String name) {
    this.id = id;
    this.type = type;
    this.name = name;
  }

  public WorkId getId() {
    return id;
  }

  public MediaType getType() {
    return type;
  }

  public String getName() {
    return name;
  }
}
