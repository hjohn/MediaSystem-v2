package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.BasicStream;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.mediamanager.BasicStreamStore;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.util.Throwables;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
public class DatabaseStreamStore implements BasicStreamStore {
  private static final Logger LOGGER = Logger.getLogger(DatabaseStreamStore.class.getName());
  private static final Set<StreamID> EMPTY_SET = Collections.emptySet();

  // Data managed by this store
  private final Map<StreamID, CachedStream> cache = new HashMap<>();

  // Indices on the data
  private final Map<StreamID, BasicStream> streamIndex = new HashMap<>();  // Contains all BasicStreams, including children
  private final Map<StreamID, StreamID> parentIndex = new HashMap<>();     // Maps child StreamID to parent StreamID
  private final Map<Identifier, Set<StreamID>> identifierIndex = new HashMap<>();

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

        badIds.add(r.getStreamId());
      }
    });

    badIds.stream().forEach(database::delete);

    LOGGER.fine("Loaded " + cache.size() + " cached stream records, deleted " + badIds.size() + " bad ones");
  }

  synchronized Map<StreamID, BasicStream> findByImportSourceId(long importSourceId) {
    return cache.values().stream()
      .filter(cs -> cs.getImportSourceId() == importSourceId)
      .map(CachedStream::getIdentifiedStream)
      .map(IdentifiedStream::getStream)
      .collect(Collectors.toMap(BasicStream::getId, Function.identity()));
  }

  @Override
  public synchronized Set<BasicStream> findStreams(Identifier identifier) {
    return identifierIndex.getOrDefault(identifier, EMPTY_SET).stream()
      .map(sid -> cache.get(sid))
      .map(CachedStream::getIdentifiedStream)
      .map(IdentifiedStream::getStream)
      .collect(Collectors.toSet());
  }

  public synchronized Map<BasicStream, Identification> findStreamsAndIdentifications(Identifier identifier) {
    return identifierIndex.getOrDefault(identifier, EMPTY_SET).stream()
      .map(sid -> cache.get(sid))
      .map(CachedStream::getIdentifiedStream)
      .collect(Collectors.toMap(IdentifiedStream::getStream, is -> is.getIdentifications().get(identifier)));
  }

  private Stream<BasicStream> stream(MediaType type, String tag) {
    return cache.values().stream()
      .filter(cs -> tag == null ? true : importSourceProvider.getStreamSource(cs.getImportSourceId() & 0xffff).getStreamSource().getTags().contains(tag))  // TODO performance here might suck somewhat
      .map(CachedStream::getIdentifiedStream)
      .map(IdentifiedStream::getStream)
      .filter(s -> s.getType().equals(type));
  }

  @Override
  public synchronized Set<BasicStream> findStreams(MediaType type, String tag) {
    return stream(type, tag)
      .collect(Collectors.toSet());
  }

  @Override
  public synchronized Map<BasicStream, Map<Identifier, Identification>> findIdentifiersByStreams(MediaType type, String tag) {
    return stream(type, tag)
      .collect(Collectors.toMap(Function.identity(), s -> findIdentifications(s.getId(), type)));
  }

  @Override
  public synchronized Optional<BasicStream> findStream(StreamID streamId) {
    return Optional.ofNullable(streamIndex.get(streamId));
  }

  @Override
  public synchronized Optional<StreamID> findParentId(StreamID streamId) {
    return Optional.ofNullable(parentIndex.get(streamId));
  }

  @Override
  public synchronized StreamSource findStreamSource(StreamID streamId) {
    CachedStream cachedStream = cache.get(streamId);

    if(cachedStream == null) {
      return null;
    }

    return importSourceProvider.getStreamSource(cache.get(streamId).getImportSourceId() & 0xffff).getStreamSource();
  }

  public synchronized Map<Identifier, Identification> findIdentifications(StreamID streamId, MediaType mediaType) {
    CachedStream cachedStream = cache.get(streamId);

    if(cachedStream == null) {
      return null;
    }

    return cachedStream.getIdentifiedStream().getIdentifications().entrySet().stream().filter(e -> e.getKey().getDataSource().getType().equals(mediaType)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public synchronized Map<Identifier, Identification> findIdentifications(StreamID streamId) {
    CachedStream cachedStream = cache.get(streamId);

    if(cachedStream == null) {
      return Collections.emptyMap();
    }

    return cachedStream.getIdentifiedStream().getIdentifications();
  }

  @Override
  public synchronized List<BasicStream> findNewest(int maximum) {
    return cache.values().stream()
      .sorted(Comparator.comparing(CachedStream::getCreationTime).reversed())
      .limit(maximum)
      .map(CachedStream::getIdentifiedStream)
      .map(IdentifiedStream::getStream)
      .collect(Collectors.toList());
  }

  public synchronized Optional<Instant> findCreationTime(StreamID streamId) {
    return Optional.ofNullable(cache.get(findParentId(streamId).orElse(streamId))).map(CachedStream::getCreationTime);
  }

  synchronized Set<StreamID> findUnenrichedStreams() {
    return cache.values().stream()
      .filter(cs -> cs.getLastEnrichTime() == null)
      .map(CachedStream::getIdentifiedStream)
      .map(IdentifiedStream::getStream)
      .map(BasicStream::getId)
      .collect(Collectors.toSet());
  }

  synchronized Set<StreamID> findStreamsNeedingRefresh(int maximum) {
    Instant now = Instant.now();

    return cache.values().stream()
      .filter(cs -> cs.getNextEnrichTime() != null && cs.getNextEnrichTime().isBefore(now))
      .limit(maximum)
      .map(CachedStream::getIdentifiedStream)
      .map(IdentifiedStream::getStream)
      .map(BasicStream::getId)
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
      cs.getIdentifiedStream(),
      cs.getImportSourceId(),
      cs.getCreationTime(),
      Instant.now(),
      nextEnrichTime
    );

    removeFromCache(streamId);
    putInCache(newCS);

    database.store(codec.toRecord(newCS));
  }

  synchronized void remove(StreamID streamId) {
    database.delete(streamId.asInt());
    removeFromCache(streamId);
  }

  synchronized void put(int importSourceId, BasicStream stream) {
    CachedStream existingCS = cache.get(stream.getId());
    IdentifiedStream identifiedStream = new IdentifiedStream(stream, existingCS == null ? Collections.emptyMap() : existingCS.getIdentifiedStream().getIdentifications());
    CachedStream newCS = new CachedStream(identifiedStream, importSourceId, existingCS == null ? Instant.now() : existingCS.getCreationTime(), null, null);

    database.store(codec.toRecord(newCS));

    removeFromCache(stream.getId());
    putInCache(newCS);
  }

  synchronized void putIdentifications(StreamID streamId, Map<Identifier, Identification> identifications) {
    CachedStream cs = cache.get(streamId);

    if(cs != null) {
      CachedStream newCS = new CachedStream(
        new IdentifiedStream(cs.getIdentifiedStream().getStream(), identifications),
        cs.getImportSourceId(),
        cs.getCreationTime(),
        cs.getLastEnrichTime(),
        cs.getNextEnrichTime()
      );

      database.store(codec.toRecord(newCS));

      removeFromCache(streamId);
      putInCache(newCS);
    }
  }

  private void removeFromCache(StreamID streamId) {
    CachedStream cs = cache.remove(streamId);

    if(cs != null) {
      IdentifiedStream is = cs.getIdentifiedStream();

      // Remove stream from identifier index
      is.getIdentifications().keySet().forEach(
        identifier -> identifierIndex.computeIfPresent(identifier, (k, v) -> v.remove(streamId) && v.isEmpty() ? null : v)
      );

      // Remove stream from stream id index
      streamIndex.remove(is.getStream().getId());

      // Remove child streams from stream id index
      is.getStream().getChildren().forEach(c -> {
        // Remove child stream from stream id index
        streamIndex.remove(c.getId());

        // Remove child stream id from parent stream id index
        parentIndex.remove(c.getId());
      });
    }
  }

  private void putInCache(CachedStream cs) {
    IdentifiedStream is = cs.getIdentifiedStream();
    StreamID streamId = is.getStream().getId();

    cache.put(streamId, cs);

    // Add stream to identifier index
    is.getIdentifications().keySet().forEach(i -> identifierIndex.computeIfAbsent(i, k -> new HashSet<>()).add(streamId));

    // Add stream to stream id index
    streamIndex.put(is.getStream().getId(), is.getStream());

    is.getStream().getChildren().forEach(c -> {
      // Add child stream to stream id index
      streamIndex.put(c.getId(), c);

      // Add child stream id to parent stream id index
      parentIndex.put(c.getId(), streamId);
    });
  }
}
