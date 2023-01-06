package hs.mediasystem.util.events;

import hs.mediasystem.util.events.store.EventStore.EventEnvelope;
import hs.mediasystem.util.events.streams.Source;

public interface EventSelector<T> {
  Source<EventEnvelope<T>> from(long offset);
  Source<T> plain();
}
