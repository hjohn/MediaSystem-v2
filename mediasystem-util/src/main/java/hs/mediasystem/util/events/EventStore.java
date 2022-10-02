package hs.mediasystem.util.events;

public interface EventStore<T> {

  record EventEnvelope<T>(long index, T event) {}

  /**
   * Appends the given event.
   *
   * @param event an event to append, cannot be {@code null}
   * @return the index of the newly appended event
   */
  long append(T event);

  /**
   * Gets the first event available starting from the given
   * index (inclusive). Blocks until an event becomes available.
   *
   * @param fromIndex an index
   * @return an {@link EventEnvelope} containing the next event, never {@code null}
   * @throws InterruptedException when the thread was interrupted
   */
  EventEnvelope<T> take(long fromIndex) throws InterruptedException;

  /**
   * Gets the first event available starting from the given
   * index (inclusive) if one is available immediately, otherwise
   * returns {@code null}.
   *
   * @param fromIndex an index
   * @return an {@link EventEnvelope} containing the next event, or {@code null} if none was available
   */
  EventEnvelope<T> poll(long fromIndex);
}
