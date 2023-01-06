package hs.mediasystem.util;

import java.util.function.Consumer;

public class NamedConsumer<T> implements Consumer<T> {
  private final String name;
  private final Consumer<? super T> consumer;

  public NamedConsumer(String name, Consumer<? super T> consumer) {
    this.name = name;
    this.consumer = consumer;
  }

  @Override
  public void accept(T t) {
    consumer.accept(t);
  }

  @Override
  public String toString() {
    return name;
  }
}
