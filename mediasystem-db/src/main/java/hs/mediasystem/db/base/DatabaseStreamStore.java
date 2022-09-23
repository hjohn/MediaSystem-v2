package hs.mediasystem.db.base;

import hs.ddif.annotations.Produces;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrintProvider;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.mediamanager.StreamableStore;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.events.EventSource;
import hs.mediasystem.util.events.EventStream;
import hs.mediasystem.util.events.InMemoryEventStream;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DatabaseStreamStore implements StreamableStore {
  private static final Logger LOGGER = Logger.getLogger(DatabaseStreamStore.class.getName());

  private final EventStream<StreamableEvent> eventStream = new InMemoryEventStream<>();

  // Data managed by this store
  private final Map<StreamID, CachedStream> cache = new HashMap<>();

  // Indices on the data
  private final Map<WorkId, List<StreamID>> workIdIndex = new HashMap<>();
  private final Map<StreamID, List<StreamID>> childIndex = new HashMap<>();  // maps a parent id to list of children
  private final Map<ContentID, List<StreamID>> contentIdIndex = new HashMap<>();

  @Inject private ImportSourceProvider importSourceProvider;
  @Inject private StreamDatabase database;
  @Inject private CachedStreamCodec codec;
  @Inject private ContentPrintProvider contentPrintProvider;

  @Produces
  EventSource<StreamableEvent> streamableEvents() {
    return eventStream;
  }

  @PostConstruct
  private void postConstruct() {
    List<Integer> badIds = new ArrayList<>();

    database.forEach(r -> {
      try {
        CachedStream cs = codec.fromRecord(r);

        if(importSourceProvider.getImportSource(cs.getImportSourceId()) != null) {
          putInCache(cs);
        }
        else {
          badIds.add(r.getId());
        }
      }
      catch(IOException e) {
        LOGGER.warning("Exception decoding record: " + r + ": " + Throwables.formatAsOneLine(e));

        badIds.add(r.getId());
      }
    });

    // There is an order to follow; child streams should be after their parent streams

    // parents first:
    for(StreamID id : cache.keySet()) {
      if(childIndex.containsKey(id)) {
        eventStream.push(new StreamableEvent.Updated(cache.get(id)));
      }
    }

    // children:
    for(StreamID id : cache.keySet()) {
      if(!childIndex.containsKey(id)) {
        eventStream.push(new StreamableEvent.Updated(cache.get(id)));
      }
    }

    badIds.stream().forEach(database::delete);

    LOGGER.fine("Loaded " + cache.size() + " cached stream records, deleted " + badIds.size() + " bad or unused ones");
  }

  synchronized Map<StreamID, Streamable> findByImportSourceId(long importSourceId) {
    return cache.values().stream()
      .filter(cs -> cs.getImportSourceId() == importSourceId)
      .map(CachedStream::getStreamable)
      .collect(Collectors.toMap(Streamable::getId, Function.identity()));
  }

  @Override
  public synchronized Optional<Streamable> findStream(StreamID streamId) {
    return Optional.ofNullable(cache.get(streamId)).map(CachedStream::getStreamable);
  }

  public synchronized Optional<StreamID> findParentId(StreamID streamId) {
    return findStream(streamId).flatMap(Streamable::getParentId);
  }

  @Override
  public synchronized List<Streamable> findChildren(StreamID streamId) {
    return childIndex.getOrDefault(streamId, List.of()).stream()
      .map(cache::get)
      .map(CachedStream::getStreamable)
      .collect(Collectors.toList());
  }

  public synchronized StreamSource findStreamSource(StreamID streamId) {
    CachedStream cachedStream = cache.get(streamId);

    if(cachedStream == null) {
      return null;
    }

    return importSourceProvider.getImportSource(cache.get(streamId).getImportSourceId()).getStreamSource();
  }

  public synchronized Optional<Identification> findIdentification(StreamID streamId) {
    return Optional.ofNullable(cache.get(streamId)).flatMap(CachedStream::getIdentification);
  }

  public synchronized Optional<Instant> findDiscoveryTime(StreamID streamId) {
    return Optional.ofNullable(cache.get(streamId)).map(CachedStream::getDiscoveryTime);
  }

  synchronized Optional<Instant> findLastEnrichTime(StreamID streamId) {
    return Optional.ofNullable(cache.get(streamId)).map(CachedStream::getLastEnrichTime);
  }

  synchronized Set<Streamable> findUnenrichedStreams() {
    return cache.values().stream()
      .filter(cs -> cs.getLastEnrichTime() == null)
      .map(CachedStream::getStreamable)
      .collect(Collectors.toSet());
  }

  synchronized Set<Streamable> findStreamsNeedingRefresh(int maximum) {
    Instant now = Instant.now();

    return cache.values().stream()
      .filter(cs -> cs.getNextEnrichTime() != null && cs.getNextEnrichTime().isBefore(now))
      .limit(maximum)
      .map(CachedStream::getStreamable)
      .collect(Collectors.toSet());
  }

  synchronized void markEnriched(StreamID streamId) {
    CachedStream cs = cache.get(streamId);

    if(cs == null) {
      return;
    }

    Instant lastEnrichTime = cs.getLastEnrichTime();
    Instant now = Instant.now();
    Instant nextEnrichTime;

    if(lastEnrichTime != null) {
      double secondsUntilNextTime = Math.abs(Duration.between(lastEnrichTime, now).toSeconds()) + 1;  // Make sure this is not zero or negative, or an infinite loop can result below

      secondsUntilNextTime *= 1.6;

      while(secondsUntilNextTime < 6 * 60 * 60) {
        secondsUntilNextTime *= (0.1 * Math.random() + 1.4);
      }

      while(secondsUntilNextTime > 30 * 24 * 60 * 60L) {
        secondsUntilNextTime *= 0.95;
      }

      nextEnrichTime = now.plusSeconds((long)secondsUntilNextTime);
    }
    else {
      nextEnrichTime = now.plusSeconds(6 * 60 * 60);
    }

    CachedStream newCS = new CachedStream(
      cs.getStreamable(),
      cs.getIdentification().orElse(null),
      cs.getDiscoveryTime(),
      Instant.now(),
      nextEnrichTime
    );

    removeFromCache(streamId);
    putInCache(newCS);

    database.store(codec.toRecord(newCS));
  }

  // Removes children as well
  synchronized void remove(StreamID streamId) {
    CachedStream cachedStream = cache.get(streamId);

    if(cachedStream != null) {
      StreamID id = cachedStream.getStreamable().getId();

      eventStream.push(new StreamableEvent.Removed(streamId));

      List.copyOf(childIndex.getOrDefault(streamId, List.of())).forEach(sid -> eventStream.push(new StreamableEvent.Removed(sid)));

      database.delete(id.getImportSourceId(), id.getContentId().asInt(), id.getName());  // this cascade deletes children
      removeAllFromCache(streamId);
    }
  }

  private void removeAllFromCache(StreamID streamId) {
    List.copyOf(childIndex.getOrDefault(streamId, List.of())).forEach(this::removeFromCache);

    removeFromCache(streamId);
  }

  synchronized void put(Streamable streamable) {
    CachedStream updatedCachedStream = toUpdatedCachedStream(streamable);

    database.store(codec.toRecord(updatedCachedStream));

    eventStream.push(new StreamableEvent.Updated(updatedCachedStream));

    removeFromCache(streamable.getId());
    putInCache(updatedCachedStream);
  }

  synchronized void putIdentification(StreamID streamId, Identification identification) {
    toIdentifiedCachedStream(streamId, identification).ifPresent(ucs -> {
      database.store(codec.toRecord(ucs));

      eventStream.push(new StreamableEvent.Updated(ucs));

      removeFromCache(streamId);
      putInCache(ucs);
    });
  }

  private CachedStream toUpdatedCachedStream(Streamable streamable) {
    CachedStream existingCS = cache.get(streamable.getId());

    if(existingCS == null) {
      return new CachedStream(streamable, null, contentPrintProvider.get(streamable.getId().getContentId()).getSignatureCreationTime(), null, null);
    }

    return new CachedStream(streamable, existingCS.getIdentification().orElse(null), existingCS.getDiscoveryTime(), null, null);  // enrich times cleared, as new stream attributes means enrichment must be done again
  }

  private Optional<CachedStream> toIdentifiedCachedStream(StreamID streamId, Identification identification) {
    CachedStream existingCS = cache.get(streamId);

    if(existingCS == null) {
      return Optional.empty();
    }

    return Optional.of(new CachedStream(existingCS.getStreamable(), identification, existingCS.getDiscoveryTime(), existingCS.getLastEnrichTime(), existingCS.getNextEnrichTime()));
  }

  private void removeFromCache(StreamID streamId) {
    CachedStream cs = cache.remove(streamId);

    if(cs != null) {
      // Remove stream from workId index
      cs.getIdentification().stream().map(Identification::getWorkIds).flatMap(Collection::stream).forEach(
        i -> workIdIndex.computeIfPresent(i, (k, v) -> v.remove(streamId) && v.isEmpty() ? null : v)
      );

      // Remove stream from content id index
      contentIdIndex.computeIfPresent(streamId.getContentId(), (k, v) -> v.remove(streamId) && v.isEmpty() ? null : v);

      // Remove stream from child index (if it has a parent)
      cs.getStreamable().getParentId().ifPresent(pid -> childIndex.computeIfPresent(pid, (k, v) -> v.remove(streamId) && v.isEmpty() ? null : v));
    }
  }

  private void putInCache(CachedStream cs) {
    StreamID streamId = cs.getStreamable().getId();

    cache.put(streamId, cs);

    // Add stream to workId index
    cs.getIdentification().stream().map(Identification::getWorkIds).flatMap(Collection::stream).forEach(
      i -> workIdIndex.computeIfAbsent(i, k -> new ArrayList<>()).add(streamId)
    );

    // Add stream to content id index
    contentIdIndex.computeIfAbsent(streamId.getContentId(), k -> new ArrayList<>()).add(streamId);

    // Add stream to child index (if it has a parent)
    cs.getStreamable().getParentId().ifPresent(pid -> childIndex.computeIfAbsent(pid, k -> new ArrayList<>()).add(streamId));
  }
}
