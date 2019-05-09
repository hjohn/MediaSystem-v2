package hs.mediasystem.db;

import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.scanner.api.StreamPrint;
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
  public <T> T getOrDefault(StreamPrint streamPrint, String key, T defaultValue) {
    StreamState streamState = getStreamState(streamPrint);

    return (T)streamState.getProperties().getOrDefault(key, defaultValue);
  }

  public void put(StreamPrint streamPrint, String key, Object value) {
    StreamState streamState = getStreamState(streamPrint);

    streamState.getProperties().put(key, value);

    // Make a copy to save, as the entry can be updated asynchronously while this one is being saved:
    StreamState copy = new StreamState(streamState.getStreamID(), new HashMap<>(streamState.getProperties()));

    EXECUTOR.execute(() -> store.store(copy));
  }

  private StreamState getStreamState(StreamPrint streamPrint) {
    return streamStates.computeIfAbsent(streamPrint.getId(), k -> new StreamState(streamPrint.getId(), new HashMap<>()));
  }
}
