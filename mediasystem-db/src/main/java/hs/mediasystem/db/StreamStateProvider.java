package hs.mediasystem.db;

import hs.mediasystem.ext.basicmediatypes.scan.StreamPrint;
import hs.mediasystem.util.ByteArrays;
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

  private final Map<String, StreamState> streamStates = new HashMap<>();

  @PostConstruct
  private void postConstruct() {
    store.forEach(ss -> streamStates.put(toKey(ss.getHash(), ss.getSize(), ss.getLastModificationTime()), ss));
  }

  @SuppressWarnings("unchecked")
  public <T> T getOrDefault(StreamPrint streamPrint, String key, T defaultValue) {
    StreamState streamState = getStreamState(streamPrint);

    return (T)streamState.getProperties().getOrDefault(key, defaultValue);
  }

  public void put(StreamPrint streamPrint, String key, Object value) {
    StreamState streamState = getStreamState(streamPrint);

    streamState.getProperties().put(key, value);

    EXECUTOR.execute(() -> store.store(streamState));
  }

  private StreamState getStreamState(StreamPrint streamPrint) {
    String key = toKey(streamPrint.getHash(), streamPrint.getSize(), streamPrint.getLastModificationTime());

    return streamStates.computeIfAbsent(key, k -> new StreamState(streamPrint.getHash(), streamPrint.getSize(), streamPrint.getLastModificationTime(), new HashMap<>()));
  }

  private static String toKey(byte[] hash, Long size, long lastModificationTime) {
    return ByteArrays.toHex(hash) + "-" + size + "-" + lastModificationTime;
  }
}
