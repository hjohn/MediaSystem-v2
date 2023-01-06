package hs.mediasystem.util.events.streams;

/**
 * Represents a stream of events which can have new events appended to it.
 *
 * @param <T> the event type
 */
public interface EventStream<T> extends Sink<T>, Source<T> {
}
