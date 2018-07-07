package hs.mediasystem.util;

import java.util.concurrent.LinkedBlockingDeque;

public class LifoBlockingDeque<T> extends LinkedBlockingDeque<T> {

  public LifoBlockingDeque(int capacity) {
    super(capacity);
  }

  public LifoBlockingDeque() {
    super();
  }

  @Override
  public boolean add(T t) {
    addFirst(t);
    return true;
  }

  @Override
  public boolean offer(T t) {
    return super.offerFirst(t);
  }

  @Override
  public T poll() {
    return super.pollLast();
  }

  @Override
  public T remove() {
    return super.removeFirst();
  }
}