package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrintProvider;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.mediamanager.StreamableStore;
import hs.mediasystem.util.Throwables;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DatabaseStreamStore implements StreamableStore {
  private static final Logger LOGGER = Logger.getLogger(DatabaseStreamStore.class.getName());
  private static final List<StreamID> EMPTY_LIST = Collections.emptyList();

  // Data managed by this store
  private final Map<StreamID, CachedStream> cache = new HashMap<>();

  // Indices on the data
  private final Map<Identifier, List<StreamID>> identifierIndex = new HashMap<>();
  private final Map<StreamID, List<StreamID>> childIndex = new HashMap<>();  // maps a parent id to list of children
  private final Map<ContentID, List<StreamID>> contentIdIndex = new HashMap<>();

  @Inject private ImportSourceProvider importSourceProvider;
  @Inject private StreamDatabase database;
  @Inject private CachedStreamCodec codec;
  @Inject private ContentPrintProvider contentPrintProvider;

  private final Comparator<CachedStream> reversedCreationOrder = Comparator
      .comparing(CachedStream::getDiscoveryTime)
      .thenComparingLong(cs -> contentPrintProvider.get(cs.getStreamable().getId().getContentId()).getLastModificationTime())
      .reversed();

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
  public synchronized List<Streamable> findStreams(Identifier identifier) {
    return identifierIndex.getOrDefault(identifier, EMPTY_LIST).stream()
      .map(sid -> cache.get(sid))
      .map(CachedStream::getStreamable)
      .collect(Collectors.toList());
  }

  @Override
  public synchronized List<Streamable> findStreams(ContentID contentId) {
    return contentIdIndex.getOrDefault(contentId, EMPTY_LIST).stream()
      .map(sid -> cache.get(sid))
      .map(CachedStream::getStreamable)
      .collect(Collectors.toList());
  }

  @Override
  public synchronized List<Streamable> findStreams(MediaType type, String tag) {
    return stream(type, tag)
      .collect(Collectors.toList());
  }

  private Stream<Streamable> stream(MediaType type, String tag) {
    return cache.values().stream()
      .filter(cs -> tag == null ? true : importSourceProvider.getImportSource(cs.getImportSourceId()).getStreamSource().getTags().contains(tag))  // TODO performance here might suck somewhat
      .map(CachedStream::getStreamable)
      .filter(s -> type == null ? true : s.getType().equals(type));
  }

  public synchronized List<Streamable> findRootStreams(String tag) {
    List<Streamable> collect = stream(null, tag)
      .filter(s -> s.getParentId().isEmpty())
      .collect(Collectors.toList());

    return collect;
  }

  @Override
  public synchronized Optional<Streamable> findStream(StreamID streamId) {
    return Optional.ofNullable(cache.get(streamId)).map(CachedStream::getStreamable);
  }

  @Override
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

  @Override
  public synchronized StreamSource findStreamSource(StreamID streamId) {
    CachedStream cachedStream = cache.get(streamId);

    if(cachedStream == null) {
      return null;
    }

    return importSourceProvider.getImportSource(cache.get(streamId).getImportSourceId()).getStreamSource();
  }

  @Override
  public synchronized Optional<Identification> findIdentification(StreamID streamId) {
    return Optional.ofNullable(cache.get(streamId)).flatMap(CachedStream::getIdentification);
  }

  @Override
  public synchronized List<Streamable> findNewest(int maximum, Predicate<MediaType> filter) {
    return cache.values().stream()
      .filter(cs -> filter.test(cs.getStreamable().getType()))
      .sorted(reversedCreationOrder)
      .limit(maximum)
      .map(CachedStream::getStreamable)
      .collect(Collectors.toList());
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

      while(secondsUntilNextTime < 6 * 60 * 60) {
        secondsUntilNextTime *= (0.1 * Math.random() + 1.4);
      }

      while(secondsUntilNextTime > 90 * 24 * 60 * 60L) {
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

    removeFromCache(streamable.getId());
    putInCache(updatedCachedStream);
  }

  synchronized void putIdentification(StreamID streamId, Identification identification) {
    toIdentifiedCachedStream(streamId, identification).ifPresent(ucs -> {
      database.store(codec.toRecord(ucs));

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
      // Remove stream from identifier index
      cs.getIdentification().stream().map(Identification::getIdentifiers).flatMap(Collection::stream).forEach(
        i -> identifierIndex.computeIfPresent(i, (k, v) -> v.remove(streamId) && v.isEmpty() ? null : v)
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

    // Add stream to identifier index
    cs.getIdentification().stream().map(Identification::getIdentifiers).flatMap(Collection::stream).forEach(
      i -> identifierIndex.computeIfAbsent(i, k -> new ArrayList<>()).add(streamId)
    );

    // Add stream to content id index
    contentIdIndex.computeIfAbsent(streamId.getContentId(), k -> new ArrayList<>()).add(streamId);

    // Add stream to child index (if it has a parent)
    cs.getStreamable().getParentId().ifPresent(pid -> childIndex.computeIfAbsent(pid, k -> new ArrayList<>()).add(streamId));
  }
}
