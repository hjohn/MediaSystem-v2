package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.mediamanager.StreamSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.inject.Inject;

public class DatabaseStreamStoreShim {
  private final DatabaseStreamStore store;

  @Inject
  public DatabaseStreamStoreShim(DatabaseStreamStore store) {
    this.store = store;
  }

  public List<Streamable> findStreams(Identifier identifier) {
    return store.findStreams(identifier);
  }

  public List<Streamable> findStreams(ContentID contentId) {
    return store.findStreams(contentId);
  }

  public List<Streamable> findStreams(MediaType type, String tag) {
    return store.findStreams(type, tag);
  }

  public List<Streamable> findRootStreams(String tag) {
    return store.findRootStreams(tag);
  }

  public Optional<Streamable> findStream(StreamID streamId) {
    return store.findStream(streamId);
  }

  public Optional<StreamID> findParentId(StreamID streamId) {
    return store.findParentId(streamId);
  }

  public List<Streamable> findChildren(StreamID streamId) {
    return store.findChildren(streamId);
  }

  public StreamSource findStreamSource(StreamID streamId) {
    return store.findStreamSource(streamId);
  }

  public Optional<Identification> findIdentification(StreamID streamId) {
    return store.findIdentification(streamId);
  }

  public List<Streamable> findNewest(int maximum, Predicate<MediaType> filter) {
    return store.findNewest(maximum, filter);
  }

  public Optional<Instant> findDiscoveryTime(StreamID streamId) {
    return store.findDiscoveryTime(streamId);
  }

  // package private methods made public

  public void put(Streamable streamable) {
    store.put(streamable);
  }
}
