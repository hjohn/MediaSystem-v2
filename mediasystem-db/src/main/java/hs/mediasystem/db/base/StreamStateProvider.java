package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.util.concurrent.NamedThreadFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamStateProvider {
  private static final Executor EXECUTOR = Executors.newSingleThreadExecutor(new NamedThreadFactory("StreamStateProvider", true));

  @Inject private StreamStateStore store;

  private final Map<ContentID, StreamState> streamStates = new HashMap<>();

  @PostConstruct
  private void postConstruct() {
    store.forEach(ss -> streamStates.put(ss.getContentID(), ss));
  }

  @SuppressWarnings("unchecked")
  public synchronized <T> T getOrDefault(ContentID contentId, String key, T defaultValue) {
    if(!streamStates.containsKey(contentId)) {  // Prevents modifying map if key didn't exist
      return defaultValue;
    }

    StreamState streamState = getStreamState(contentId);  // Modifies map if key didn't exist

    return (T)streamState.getProperties().getOrDefault(key, defaultValue);
  }

  public synchronized void put(ContentID contentId, String key, Object value) {
    StreamState streamState = getStreamState(contentId);

    streamState.getProperties().put(key, value);

    // Make a copy to save, as the entry can be updated asynchronously while this one is being saved:
    StreamState copy = new StreamState(streamState.getContentID(), new HashMap<>(streamState.getProperties()));

    EXECUTOR.execute(() -> store.store(copy));
  }

  public synchronized <R> R map(Function<Stream<StreamState>, R> converter) {
    return converter.apply(streamStates.values().stream());
  }

  private StreamState getStreamState(ContentID contentId) {
    return streamStates.computeIfAbsent(contentId, k -> new StreamState(contentId, new HashMap<>()));
  }
}
