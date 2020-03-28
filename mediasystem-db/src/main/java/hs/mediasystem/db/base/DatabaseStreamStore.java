package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DatabaseStreamStore implements StreamableStore {
  private static final Logger LOGGER = Logger.getLogger(DatabaseStreamStore.class.getName());
  private static final Set<ContentID> EMPTY_SET = Collections.emptySet();

  // Data managed by this store
  private final Map<ContentID, CachedStream> cache = new HashMap<>();

  // Indices on the data
  private final Map<Identifier, Set<ContentID>> identifierIndex = new HashMap<>();
  private final Map<ContentID, List<ContentID>> childIndex = new HashMap<>();  // maps a parent id to list of children

  @Inject private ImportSourceProvider importSourceProvider;
  @Inject private StreamDatabase database;
  @Inject private CachedStreamCodec codec;

  @PostConstruct
  private void postConstruct() {
    List<Integer> badIds = new ArrayList<>();

    database.forEach(r -> {
      try {
        putInCache(codec.fromRecord(r));
      }
      catch(IOException e) {
        LOGGER.warning("Exception decoding record: " + r + ": " + Throwables.formatAsOneLine(e));

        badIds.add(r.getContentId());
      }
    });

    badIds.stream().forEach(database::delete);

    LOGGER.fine("Loaded " + cache.size() + " cached stream records, deleted " + badIds.size() + " bad ones");
  }

  synchronized Map<ContentID, Streamable> findByImportSourceId(long importSourceId) {
    return cache.values().stream()
      .filter(cs -> cs.getImportSourceId() == importSourceId)
      .map(CachedStream::getStreamable)
      .collect(Collectors.toMap(Streamable::getId, Function.identity()));
  }

  @Override
  public synchronized Set<Streamable> findStreams(Identifier identifier) {
    return identifierIndex.getOrDefault(identifier, EMPTY_SET).stream()
      .map(sid -> cache.get(sid))
      .map(CachedStream::getStreamable)
      .collect(Collectors.toSet());
  }

  private Stream<Streamable> stream(MediaType type, String tag) {
    return cache.values().stream()
      .filter(cs -> tag == null ? true : importSourceProvider.getStreamSource(cs.getImportSourceId() & 0xffff).getStreamSource().getTags().contains(tag))  // TODO performance here might suck somewhat
      .map(CachedStream::getStreamable)
      .filter(s -> s.getType().equals(type));
  }

  @Override
  public synchronized Set<Streamable> findStreams(MediaType type, String tag) {
    return stream(type, tag)
      .collect(Collectors.toSet());
  }

  @Override
  public synchronized Optional<Streamable> findStream(ContentID contentId) {
    return Optional.ofNullable(cache.get(contentId)).map(CachedStream::getStreamable);
  }

  @Override
  public synchronized Optional<ContentID> findParentId(ContentID contentId) {
    return findStream(contentId).flatMap(Streamable::getParentContentId);
  }

  @Override
  public synchronized List<Streamable> findChildren(ContentID contentId) {
    return childIndex.getOrDefault(contentId, List.of()).stream()
      .map(cache::get)
      .map(CachedStream::getStreamable)
      .collect(Collectors.toList());
  }

  @Override
  public synchronized StreamSource findStreamSource(ContentID contentId) {
    CachedStream cachedStream = cache.get(contentId);

    if(cachedStream == null) {
      return null;
    }

    return importSourceProvider.getStreamSource(cache.get(contentId).getImportSourceId() & 0xffff).getStreamSource();
  }

  @Override
  public synchronized Optional<Identification> findIdentification(ContentID contentId) {
    return Optional.ofNullable(cache.get(contentId)).flatMap(CachedStream::getIdentification);
  }

  @Override
  public synchronized List<Streamable> findNewest(int maximum) {
    return cache.values().stream()
      .sorted(Comparator.comparing(CachedStream::getCreationTime).reversed())
      .limit(maximum)
      .map(CachedStream::getStreamable)
      .collect(Collectors.toList());
  }

  public synchronized Optional<Instant> findCreationTime(ContentID contentId) {
    return Optional.ofNullable(cache.get(findParentId(contentId).orElse(contentId))).map(CachedStream::getCreationTime);
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

  synchronized void markEnriched(ContentID contentId) {
    CachedStream cs = cache.get(contentId);

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
      cs.getImportSourceId(),
      cs.getCreationTime(),
      Instant.now(),
      nextEnrichTime
    );

    removeFromCache(contentId);
    putInCache(newCS);

    database.store(codec.toRecord(newCS));
  }

  // Removes children as well
  synchronized void remove(ContentID contentId) {
    if(cache.containsKey(contentId)) {
      database.delete(contentId.asInt());
      removeAllFromCache(contentId);
    }
  }

  private void removeAllFromCache(ContentID contentId) {
    List.copyOf(childIndex.getOrDefault(contentId, List.of())).forEach(this::removeFromCache);

    removeFromCache(contentId);
  }

  synchronized void put(int importSourceId, Streamable streamable) {
    CachedStream updatedCachedStream = toUpdatedCachedStream(streamable, importSourceId);

    database.store(codec.toRecord(updatedCachedStream));

    removeFromCache(streamable.getId());
    putInCache(updatedCachedStream);
  }

  synchronized void putIdentification(ContentID contentId, Identification identification) {
    toIdentifiedCachedStream(contentId, identification).ifPresent(ucs -> {
      database.store(codec.toRecord(ucs));

      removeFromCache(contentId);
      putInCache(ucs);
    });
  }

  private CachedStream toUpdatedCachedStream(Streamable streamable, int importSourceId) {
    CachedStream existingCS = cache.get(streamable.getId());

    if(existingCS == null) {
      return new CachedStream(streamable, null, importSourceId, Instant.now(), null, null);
    }

    return new CachedStream(streamable, existingCS.getIdentification().orElse(null), importSourceId, existingCS.getCreationTime(), null, null);  // enrich times cleared, as new stream attributes means enrichment must be done again
  }

  private Optional<CachedStream> toIdentifiedCachedStream(ContentID contentId, Identification identification) {
    CachedStream existingCS = cache.get(contentId);

    if(existingCS == null) {
      return Optional.empty();
    }

    return Optional.of(new CachedStream(existingCS.getStreamable(), identification, existingCS.getImportSourceId(), existingCS.getCreationTime(), existingCS.getLastEnrichTime(), existingCS.getNextEnrichTime()));
  }

  private void removeFromCache(ContentID contentId) {
    CachedStream cs = cache.remove(contentId);

    if(cs != null) {
      // Remove stream from identifier index
      cs.getIdentification().stream().map(Identification::getIdentifiers).flatMap(Collection::stream).forEach(
        i -> identifierIndex.computeIfPresent(i, (k, v) -> v.remove(contentId) && v.isEmpty() ? null : v)
      );

      // Remove stream from child index (if it has a parent)
      cs.getStreamable().getParentContentId().ifPresent(pid -> childIndex.computeIfPresent(pid, (k, v) -> v.remove(cs.getStreamable().getId()) && v.isEmpty() ? null : v));
    }
  }

  private void putInCache(CachedStream cs) {
    ContentID contentId = cs.getStreamable().getId();

    cache.put(contentId, cs);

    // Add stream to identifier index
    cs.getIdentification().stream().map(Identification::getIdentifiers).flatMap(Collection::stream).forEach(
      i -> identifierIndex.computeIfAbsent(i, k -> new HashSet<>()).add(contentId)
    );

    // Add stream to child index (if it has a parent)
    cs.getStreamable().getParentContentId().ifPresent(pid -> childIndex.computeIfAbsent(pid, k -> new ArrayList<>()).add(cs.getStreamable().getId()));
  }
}
