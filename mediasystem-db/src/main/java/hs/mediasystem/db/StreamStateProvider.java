package hs.mediasystem.db;

import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.NamedThreadFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamStateProvider {
  private static final Executor EXECUTOR = Executors.newSingleThreadExecutor(new NamedThreadFactory("StreamStateProvider", true));

  @Inject private StreamStateStore store;

  private final Map<StreamID, StreamState> streamStates = new HashMap<>();

  @PostConstruct
  private void postConstruct() {
    store.forEach(ss -> streamStates.put(ss.getStreamID(), ss));
  }

  @SuppressWarnings("unchecked")
  public synchronized <T> T getOrDefault(StreamID streamId, String key, T defaultValue) {
    if(!streamStates.containsKey(streamId)) {  // Prevents modifying map if key didn't exist
      return defaultValue;
    }

    StreamState streamState = getStreamState(streamId);  // Modifies map if key didn't exist

    return (T)streamState.getProperties().getOrDefault(key, defaultValue);
  }

  public synchronized void put(StreamID streamId, String key, Object value) {
    StreamState streamState = getStreamState(streamId);

    streamState.getProperties().put(key, value);

    // Make a copy to save, as the entry can be updated asynchronously while this one is being saved:
    StreamState copy = new StreamState(streamState.getStreamID(), new HashMap<>(streamState.getProperties()));

    EXECUTOR.execute(() -> store.store(copy));
  }

  private StreamState getStreamState(StreamID streamId) {
    return streamStates.computeIfAbsent(streamId, k -> new StreamState(streamId, new HashMap<>()));
  }
}
