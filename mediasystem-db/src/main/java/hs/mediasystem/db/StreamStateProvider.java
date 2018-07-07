package hs.mediasystem.db;

import hs.mediasystem.ext.basicmediatypes.StreamPrint;
import hs.mediasystem.util.ByteArrays;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamStateProvider {
  @Inject private StreamStateStore store;

  private final Map<String, StreamState> streamStates = new HashMap<>();

  @PostConstruct
  private void postConstruct() {
    store.forEach(ss -> streamStates.put(toKey(ss.getHash(), ss.getSize(), ss.getLastModificationTime()), ss));
  }

  public Map<String, Object> get(StreamPrint streamPrint) {
    StreamState streamState = streamStates.computeIfAbsent(toKey(streamPrint.getHash(), streamPrint.getSize(), streamPrint.getLastModificationTime()), k -> new StreamState(streamPrint.getHash(), streamPrint.getSize(), streamPrint.getLastModificationTime(), new HashMap<>()));
    ObservableMap<String, Object> observableMap = FXCollections.observableMap(streamState.getProperties());

    observableMap.addListener((Observable obs) -> update(streamState));

    return observableMap;
  }

  private void update(StreamState streamState) {
    store.store(streamState);
  }

  private static String toKey(byte[] hash, Long size, long lastModificationTime) {
    return ByteArrays.toHex(hash) + "-" + size + "-" + lastModificationTime;
  }
}
