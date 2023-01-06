package hs.mediasystem.util.events;

import hs.mediasystem.util.events.streams.EventStream;
import hs.mediasystem.util.events.streams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SynchronousEventStream<T> implements EventStream<T> {
  private final List<Consumer<? super T>> subscribers = new ArrayList<>();

  @Override
  public void push(T event) {
    push(List.of(event));
  }

  @Override
  public void push(List<T> events) {
    for(T event : events) {
      for(Consumer<? super T> subscriber : subscribers) {
        subscriber.accept(event);
      }
    }
  }

  @Override
  public Subscription subscribe(Consumer<? super T> consumer) {
    subscribers.add(consumer);

    return new Subscription() {
      private boolean unsubscribed;

      @Override
      public synchronized void join() {
        if(unsubscribed) {
          throw new IllegalStateException("Not subscribed");
        }
      }

      @Override
      public synchronized void unsubscribe() {
        subscribers.remove(consumer);
        unsubscribed = true;
      }
    };
  }
}
