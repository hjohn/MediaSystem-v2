package hs.mediasystem.util;

import java.util.function.Consumer;

public abstract class AbstractNamedConsumer<T> implements Consumer<T> {
  private final String name;

  public AbstractNamedConsumer(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
