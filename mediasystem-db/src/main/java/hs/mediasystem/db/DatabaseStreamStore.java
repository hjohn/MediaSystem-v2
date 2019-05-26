package hs.mediasystem.db;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.mediamanager.BasicStreamStore;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.Throwables;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

  private final Map<StreamID, CachedStream> cache = new HashMap<>();
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

  public synchronized Map<StreamID, BasicStream> findByScannerId(long scannerId) {
    return cache.values().stream()
      .filter(cs -> cs.getScannerId() == scannerId)
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

  private Stream<BasicStream> stream(MediaType type, String tag) {
    return cache.values().stream()
      .filter(cs -> tag == null ? true : importSourceProvider.getStreamSource(cs.getScannerId() & 0xffff).getStreamSource().getTags().contains(tag))  // TODO performance here might suck somewhat
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
      .collect(Collectors.toMap(Function.identity(), s -> findIdentifications(s.getId())));
  }

  @Override
  public synchronized BasicStream findStream(StreamID streamId) {
    return cache.get(streamId).getIdentifiedStream().getStream();
  }

  @Override
  public synchronized StreamSource findStreamSource(StreamID streamId) {
    return importSourceProvider.getStreamSource(cache.get(streamId).getScannerId() & 0xffff).getStreamSource();
  }

  @Override
  public synchronized Map<Identifier, Identification> findIdentifications(StreamID streamId) {
    CachedStream cachedStream = cache.get(streamId);

    if(cachedStream == null) {
      return null;
    }

    return cachedStream.getIdentifiedStream().getIdentifications();
  }

  public synchronized Set<StreamID> findUnenrichedStreams() {
    return cache.values().stream()
      .filter(cs -> cs.getLastEnrichTime() == null)
      .map(CachedStream::getIdentifiedStream)
      .map(IdentifiedStream::getStream)
      .map(BasicStream::getId)
      .collect(Collectors.toSet());
  }

  public synchronized Set<StreamID> findStreamsNeedingRefresh(int maximum) {
    Instant now = Instant.now();

    return cache.values().stream()
      .filter(cs -> cs.getNextEnrichTime() != null && cs.getNextEnrichTime().isBefore(now))
      .limit(maximum)
      .map(CachedStream::getIdentifiedStream)
      .map(IdentifiedStream::getStream)
      .map(BasicStream::getId)
      .collect(Collectors.toSet());
  }

  public void markEnriched(StreamID streamId) {
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
      cs.getScannerId(),
      Instant.now(),
      nextEnrichTime
    );

    removeFromCache(streamId);
    putInCache(newCS);

    database.store(codec.toRecord(newCS));
  }

  public synchronized void remove(StreamID streamId) {
    database.delete(streamId.asInt());
    removeFromCache(streamId);
  }

  public synchronized void add(int scannerId, BasicStream stream) {
    CachedStream existingCS = cache.get(stream.getId());

    if(existingCS == null || !stream.equals(existingCS.getIdentifiedStream().getStream())) {
      IdentifiedStream identifiedStream = new IdentifiedStream(stream, existingCS == null ? Collections.emptyMap() : existingCS.getIdentifiedStream().getIdentifications());
      CachedStream newCS;

      if(existingCS == null) {
        newCS = new CachedStream(identifiedStream, scannerId, null, null);
      }
      else {
        newCS = new CachedStream(identifiedStream, scannerId, existingCS.getLastEnrichTime(), existingCS.getNextEnrichTime());
      }

      database.store(codec.toRecord(newCS));

      removeFromCache(stream.getId());
      putInCache(newCS);
    }
  }

  @Override
  public synchronized void putIdentifications(StreamID streamId, Map<Identifier, Identification> identifications) {
    CachedStream cs = cache.get(streamId);

    if(cs != null) {
      CachedStream newCS = new CachedStream(
        new IdentifiedStream(cs.getIdentifiedStream().getStream(), identifications),
        cs.getScannerId(),
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
      cs.getIdentifiedStream().getIdentifications().keySet().forEach(
        identifier -> identifierIndex.computeIfPresent(identifier, (k, v) -> v.remove(streamId) && v.isEmpty() ? null : v)
      );
    }
  }

  private void putInCache(CachedStream cs) {
    StreamID streamId = cs.getIdentifiedStream().getStream().getId();

    cache.put(streamId, cs);

    cs.getIdentifiedStream().getIdentifications().keySet().forEach(
      identifier -> identifierIndex.computeIfAbsent(identifier, k -> new HashSet<>()).add(streamId)
    );
  }
}
