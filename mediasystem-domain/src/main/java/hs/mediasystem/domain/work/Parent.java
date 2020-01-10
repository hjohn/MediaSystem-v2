package hs.mediasystem.domain.work;

import hs.mediasystem.domain.stream.MediaType;

public class Parent {
  private final WorkId id;
  private final String name;

  public Parent(WorkId id, String name) {
    this.id = id;
    this.name = name;
  }

  public WorkId getId() {
    return id;
  }

  public MediaType getType() {
    return id.getType();
  }

  public String getName() {
    return name;
  }
}
