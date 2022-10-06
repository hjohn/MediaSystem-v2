package hs.mediasystem.util.events;

/**
 * Abstraction for storage and retrieval of events.
 *
 * @param <T> the type of events stored by this event store
 */
public interface EventStore<T> {

  /**
   * An event envelope which provides additional meta information about the
   * associated event.
   *
   * @param <T> the type of events stored by this event store
   * @param index the index of the associated event, never negative
   * @param event an event of type {@code T}, never {@code null}
   */
  record EventEnvelope<T>(long index, T event) {}

  /**
   * Appends the given event.
   *
   * @param event an event to append, cannot be {@code null}
   * @return the index of the newly appended event
   * @throws NullPointerException if event is {@code null}
   */
  long append(T event);

  /**
   * Gets the first event available starting from the given
   * index (inclusive). Blocks until an event becomes available.
   *
   * @param fromIndex an index, cannot be negative
   * @return an {@link EventEnvelope} containing the next event, never {@code null}
   * @throws IllegalArgumentException when {@code fromIndex} is not positive
   * @throws InterruptedException when the thread was interrupted
   */
  EventEnvelope<T> take(long fromIndex) throws InterruptedException;

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
}
