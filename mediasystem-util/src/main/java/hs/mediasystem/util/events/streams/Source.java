package hs.mediasystem.util.events.streams;

import hs.mediasystem.util.AbstractNamedConsumer;
import hs.mediasystem.util.NamedConsumer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A source for objects of type {@code T}.
 *
 * @param <T> the type of objects the source provides
 */
public interface Source<T> {

  /**
   * Subscribes to this source calling the given consumer with each object the
   * source provides. A {@link Subscription} is returned which can be used to
   * terminate the subscription.
   *
   * @param consumer a consumer, cannot be {@code null}
   * @return a {@link Subscription}, never {@code null}
   */
  Subscription subscribe(Consumer<? super T> consumer);

  /**
   * Subscribes to this source calling the given consumer with each object the
   * source provides and refers to this subscription with the given name. A
   * {@link Subscription} is returned which can be used to terminate the subscription.
   *
   * @param name a name to use to refer to this subscription, cannot be {@code null}
   * @param consumer a consumer, cannot be {@code null}
   * @return a {@link Subscription}, never {@code null}
   */
  default Subscription subscribe(String name, Consumer<? super T> consumer) {
    return subscribe(new NamedConsumer<>(name, consumer));
  }

  /**
   * Returns a new source with a mapping applied to each object provided by this
   * source. If any objects resulting from the mapping are {@code null} they are
   * silently dropped.
   *
   * @param <U> the type of objects the mapping provides
   * @param mapper a mapping function, cannot be {@code null}
   * @return a new source with a mapping applied to each object provided by this source, never {@code null}
   */
  default <U> Source<U> map(Function<? super T, ? extends U> mapper) {
    return consumer -> Source.this.subscribe(new AbstractNamedConsumer<T>(consumer.toString()) {
      @Override
      public void accept(T t) {
        U u = mapper.apply(t);

        if(u != null) {
          consumer.accept(u);
        }
      }
    });
  }

  /**
   * Returns a new source with a mapping applied to each object provided by this
   * source. The mapping function can call the given consumer 0 or more times
   * to provide 0 or more resulting objects for each input object. If any objects
   * resulting from the mapping are {@code null} they are silently dropped.
   *
   * @param <U> the type of objects the mapping provides
   * @param mapper a mapping function, cannot be {@code null}
   * @return a new source with a mapping applied to each object provided by this source, never {@code null}
   */
  default <U> Source<U> mapMulti(BiConsumer<? super T, ? super Consumer<U>> mapper) {
    return consumer -> Source.this.subscribe(new AbstractNamedConsumer<T>(consumer.toString()) {
      @Override
      public void accept(T t) {
        mapper.accept(t, (Consumer<U>)u -> {
          if(u != null) {
            consumer.accept(u);
          }
        });
      }
    });
  }

  /**
   * Returns a new source which contains the elements from this source and the given
   * source. The order is preserved for each source, but the two sources can be mixed
   * arbitrarily.
   *
   * @param source a source to merge with this source, cannot be {@code null}
   * @return a new source which contains the elements from this source and the given source, never {@code null}
   */
  default Source<T> mergeWith(Source<? extends T> source) {
    return new Source<>() {
      @Override
      public Subscription subscribe(Consumer<? super T> consumer) {
        Consumer<T> synchronousConsumer = new AbstractNamedConsumer<>(consumer.toString()) {
          @Override
          public synchronized void accept(T t) {
            consumer.accept(t);
          }
        };

        Subscription sub1 = Source.this.subscribe(synchronousConsumer);
        Subscription sub2 = source.subscribe(synchronousConsumer);

        return new Subscription() {
          @Override
          public void join() {
            // not entirely sure this is correct behavior
            sub1.join();
            sub2.join();
          }

          @Override
          public void unsubscribe() {
            sub1.unsubscribe();
            sub2.unsubscribe();
          }
        };
      }
    };
  }
}
