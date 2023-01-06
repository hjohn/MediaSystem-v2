package hs.mediasystem.util.events.store;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Abstraction for storage and retrieval of events.
 *
 * @param <T> the type of events stored by this event store
 */
public interface EventStore<T> {

  /**
   * Returns the type of event stored.
   *
   * @return the type of event stored, never {@code null}
   */
  Class<T> eventType();

  /**
   * An event envelope which provides additional meta information about the
   * associated event.
   *
   * @param <T> the type of events stored by this event store
   * @param index the index of the associated event, never negative
   * @param event an event of type {@code T}, never {@code null}
   */
  record EventEnvelope<T>(long index, T event) {}

  interface Appender<T> {
    void append(T event) throws Exception;
  }

  interface Callback<T> {
    void accept(Appender<T> appender) throws Exception;
  }

  /**
   * Appends zero or more events provided by the given {@code callback}, and calls the
   * given {code onSuccess} consumer when it successfully completes.
   *
   * @param callback a {@link Callback} that is called through which events can be appended, cannot be {@code null}
   * @param onSuccess a consumer called when the append completes with its index, cannot be {@code null}
   * @throws NullPointerException if event is {@code null}
   */
  void append(Callback<T> callback, Consumer<Long> onSuccess);

  /**
   * Appends zero or more events provided by the given {@code callback}.
   *
   * @param callback a {@link Callback} that is called through which events can be appended, cannot be {@code null}
   * @throws NullPointerException if event is {@code null}
   */
  default void append(Callback<T> callback) {
    append(callback, x -> {});
  }

  /**
   * Appends the given event and calls the given {code onSuccess} consumer
   * when it successfully completes.
   *
   * @param event an event to append, cannot be {@code null}
   * @param onSuccess a consumer called when the append completes with its index, cannot be {@code null}
   * @throws NullPointerException if event is {@code null}
   */
  default void append(T event, Consumer<Long> onSuccess) {
    append(appender -> appender.append(event), onSuccess);
  }

  /**
   * Appends the given event.
   *
   * @param event an event to append, cannot be {@code null}
   * @throws NullPointerException if event is {@code null}
   */
  default void append(T event) {
    append(event, x -> {});
  }

  /**
   * Gets the first event available starting from the given
   * index (inclusive). Blocks until an event becomes available.
   *
   * @param fromIndex an index, cannot be negative
   * @return an {@link EventEnvelope} containing the next event, never {@code null}
   * @throws IllegalArgumentException when {@code fromIndex} is negative
   * @throws InterruptedException when the thread was interrupted
   */
  EventEnvelope<T> take(long fromIndex) throws InterruptedException;

  /**
   * Gets the first events available starting from the given
   * index (inclusive) up to a maximum of {@code max}. Blocks until
   * at least one event becomes available.
   *
   * @param fromIndex an index, cannot be negative
   * @param max the maximum amount of events to return, must be positive
   * @return a list of {@link EventEnvelope}s containing the next events, never {@code null} or empty
   * @throws IllegalArgumentException when {@code fromIndex} is negative
   * @throws IllegalArgumentException when {@code max} is not positive
   * @throws InterruptedException when the thread was interrupted
   */
  default List<EventEnvelope<T>> take(long fromIndex, int max) throws InterruptedException {
    List<EventEnvelope<T>> results = new ArrayList<>();

    results.add(take(fromIndex));

    for(int i = 1; i < max; i++) {
      EventEnvelope<T> envelope = poll(results.get(i - 1).index + 1);

      if(envelope == null) {
        break;
      }

      results.add(envelope);
    }

    return results;
  }

  /**
   * Gets the first event available starting from the given
   * index (inclusive) if one is available immediately, otherwise
   * returns {@code null}.
   *
   * @param fromIndex an index, cannot be negative
   * @return an {@link EventEnvelope} containing the next event, or {@code null} if none was available
   * @throws IllegalArgumentException when {@code fromIndex} is not positive
   */
  EventEnvelope<T> poll(long fromIndex);

  /**
   * Gets the first events available starting from the given index (inclusive)
   * up to the given maximum, if one or more is available immediately.
   *
   * @param fromIndex an index, cannot be negative
   * @param max a maximum, cannot be not positive
   * @return an {@link EventEnvelope} containing the next event, or {@code null} if none was available
   * @throws IllegalArgumentException when {@code fromIndex} is negative or {@code max} is not positive
   */
  default List<EventEnvelope<T>> poll(long fromIndex, int max) {
    List<EventEnvelope<T>> results = new ArrayList<>();
    long previousIndex = fromIndex - 1;

    for(int i = 0; i < max; i++) {
      EventEnvelope<T> envelope = poll(previousIndex + 1);

      if(envelope == null) {
        break;
      }

      results.add(envelope);

      previousIndex = envelope.index;
    }

    return results;
  }
}
