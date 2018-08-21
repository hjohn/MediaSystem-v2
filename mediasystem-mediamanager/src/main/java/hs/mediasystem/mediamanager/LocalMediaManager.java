package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Type;
import hs.mediasystem.ext.basicmediatypes.scan.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.scan.MediaRecord;
import hs.mediasystem.ext.basicmediatypes.scan.MediaStream;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService.Result;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.NamedThreadFactory;
import hs.mediasystem.util.StringURI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalMediaManager {
  private static final Logger LOGGER = Logger.getLogger(LocalMediaManager.class.getName());
  private static final Executor EXECUTOR = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("LocalMediaManager", true));

  @Inject private List<IdentificationService> identificationServices;
  @Inject private List<QueryService<?>> queryServices;

  private final Map<DataSource, IdentificationService> identificationServicesByDataSource = new HashMap<>();
  private final Map<DataSource, QueryService<?>> queryServicesByDataSource = new HashMap<>();
  private final List<LocalMediaListener> localMediaListeners = new ArrayList<>();

  // Various MediaStream indices
  private final Map<Type, Map<String, MediaStream<? extends MediaDescriptor>>> streamsByPrintIdentifierByType = new HashMap<>();  // By Type, then by StreamPrint.Identifier
  private final Map<Identifier, Set<MediaStream<?>>> streamsByIdentifier = new HashMap<>();
  private final Map<StringURI, Set<MediaStream<?>>> streamsByParentURI = new HashMap<>();

  @PostConstruct
  private void postConstruct() {
    LOGGER.info("Instantiated with " + identificationServices.size() + " identification services and " + queryServices.size() + " query services");

    for(IdentificationService service : identificationServices) {
      if(identificationServicesByDataSource.put(service.getDataSource(), service) != null) {
        LOGGER.warning("Multiple identification services available for datasource: " + service.getDataSource());
      }
    }
    for(QueryService<?> service : queryServices) {
      if(queryServicesByDataSource.put(service.getDataSource(), service) != null) {
        LOGGER.warning("Multiple query services available for datasource: " + service.getDataSource());
      }
    }
  }

  public <D extends MediaDescriptor> MediaStream<D> add(MediaStream<D> mediaStream) {
    MediaStream<D> replacedStream = addInternal(mediaStream);

    if(replacedStream != null) {
      localMediaListeners.stream().forEach(l -> l.mediaRemoved(replacedStream));
    }

    localMediaListeners.stream().forEach(l -> l.mediaAdded(mediaStream));

    return replacedStream;
  }

  public <D extends MediaDescriptor> boolean remove(MediaStream<D> mediaStream) {
    return removeMediaStream(mediaStream) != null;
  }

  public void addListener(LocalMediaListener listener) {
    localMediaListeners.add(listener);
  }

  public void removeListener(LocalMediaListener listener) {
    localMediaListeners.remove(listener);
  }

  private <D extends MediaDescriptor> MediaStream<D> addInternal(MediaStream<D> mediaStream) {
    synchronized(this) {
      MediaStream<D> replacedStream = removeMediaStream(mediaStream);

      addMediaStream(mediaStream);
      asyncEnrichMediaStream(mediaStream);

      return replacedStream;
    }
  }

  public synchronized <T extends MediaDescriptor> Collection<MediaStream<T>> findAllByType(Type type) {
    @SuppressWarnings("unchecked")  // Save, as the type guarantees the kind of items in the map
    Map<String, MediaStream<T>> map = (Map<String, MediaStream<T>>)(Map<?, ?>)streamsByPrintIdentifierByType.get(type);

    return map == null ? Collections.emptyList() : map.values();
  }


  public synchronized Set<MediaStream<?>> findChildren(StringURI parentUri) {
    return streamsByParentURI.getOrDefault(parentUri, Collections.emptySet());
  }

  public synchronized Set<MediaStream<?>> find(ProductionIdentifier identifier) {
    Set<MediaStream<?>> set = streamsByIdentifier.get(identifier);

    return set == null ? Collections.emptySet() : new HashSet<>(set);
  }

  // Method blocks
  public <D extends MediaDescriptor> List<Exceptional<MediaRecord<D>>> reidentify(MediaStream<D> mediaStream) {
    List<Exceptional<MediaRecord<D>>> results = identify(mediaStream, false);

    replaceMediaRecords(mediaStream, results.stream().flatMap(Exceptional::ignoreAllAndStream).collect(Collectors.toList()));

    return results;
  }

  // Method blocks
  public <D extends MediaDescriptor> List<Exceptional<MediaRecord<D>>> incrementalIdentify(MediaStream<D> mediaStream) {
    List<Exceptional<MediaRecord<D>>> results = identify(mediaStream, true);

    mergeMediaRecords(mediaStream, results.stream().flatMap(Exceptional::ignoreAllAndStream).collect(Collectors.toList()));

    return results;
  }

  // Method may block
  private <D extends MediaDescriptor> List<Exceptional<MediaRecord<D>>> identify(MediaStream<D> mediaStream, boolean incremental) {
    Type type = mediaStream.getType();

    // Create list of already identified datasources:
    Set<DataSource> identifiedDataSources = !incremental ? new HashSet<>() : mediaStream.getMediaRecords().keySet().stream()
      .map(Identifier::getDataSource)
      .collect(Collectors.toCollection(HashSet::new));

    // Get new identifications:
    List<Exceptional<Identification>> newIdentifications = identificationServicesByDataSource.entrySet().stream()
      .filter(e -> e.getKey().getType().equals(type))
      .filter(e -> !identifiedDataSources.contains(e.getKey()))  // Don't identify ones that exist already
      .map(e -> Exceptional.from(() -> Optional.ofNullable(e.getValue().identify(mediaStream.getAttributes())).orElseThrow(() -> new UnknownStreamException(mediaStream, e.getValue()))))
      .collect(Collectors.toList());

    // Get old identifications with missing queries, but skip if there is no matching query service anyway:
    List<Exceptional<Identification>> identificationsMissingQueries = !incremental ? Collections.emptyList() : mediaStream.getMediaRecords().values().stream()
      .filter(v -> v.getMediaDescriptor() == null)
      .filter(v -> queryServicesByDataSource.get(v.getDataSource()) != null)
      .map(MediaRecord::getIdentification)
      .map(Exceptional::of)
      .collect(Collectors.toList());

    List<Exceptional<Wrapper<D>>> results = new ArrayList<>();

    // Concat new identifications and existing ones with missing data into new list:
    List<Exceptional<Identification>> identificationsToQuery = Stream.of(newIdentifications, identificationsMissingQueries).flatMap(Collection::stream).collect(Collectors.toList());

    // Loop here as queries may return new Identifications that in turn require querying:
    while(!identificationsToQuery.isEmpty()) {
      identificationsToQuery
        .forEach(identificationExceptional -> results.add(
          identificationExceptional
            .map(i -> (QueryService<D>)queryServicesByDataSource.get(i.getIdentifier().getDataSource()))
            .map(qs -> qs.query(identificationExceptional.get().getIdentifier()))  // get() is safe here, won't get here if not present
            .map(r -> new Wrapper<>(identificationExceptional.get(), r))
            .or(() -> Exceptional.of(new Wrapper<D>(identificationExceptional.get(), null)))
      ));

      // Update identifiedDataSources:
      identificationsToQuery.forEach(exI -> exI.map(Identification::getIdentifier)
        .map(Identifier::getDataSource)
        .ignore(Throwable.class)
        .ifPresent(identifiedDataSources::add)
      );

      // Check Results for new Identifications:
      identificationsToQuery = results.stream()
        .map(e -> e.map(w -> w.result))
        .flatMap(Exceptional::ignoreAllAndStream)
        .map(Result::getNewIdentifications)
        .flatMap(Collection::stream)
        .filter(i -> !identifiedDataSources.contains(i.getIdentifier().getDataSource()))  // Don't identify ones that exist already (prevent id loops)
        .map(Exceptional::of)
        .collect(Collectors.toList());
    }

    return results.stream()
      .map(e -> e.map(w -> new MediaRecord<>(w.identification, w.result == null ? null : w.result.getMediaDescriptor())))
      .collect(Collectors.toList());
  }

  // TODO consider passing Identification to QueryService so it can be associated with the result for easier streams...
  private class Wrapper<D extends MediaDescriptor> {
    public final Identification identification;
    public final Result<D> result;

    public Wrapper(Identification identification, Result<D> result) {
      this.identification = identification;
      this.result = result;
    }
  }

  private <D extends MediaDescriptor> void mergeMediaRecords(MediaStream<D> mediaStream, List<MediaRecord<D>> records) {
    synchronized(this) {
      removeMediaStream(mediaStream);

      for(MediaRecord<D> mediaRecord : records) {
        mediaStream.mergeMediaRecord(mediaRecord);
      }

      addMediaStream(mediaStream);
    }

    localMediaListeners.stream().forEach(l -> l.mediaUpdated(mediaStream));
  }

  private <D extends MediaDescriptor> void replaceMediaRecords(MediaStream<D> mediaStream, List<MediaRecord<D>> records) {
    synchronized(this) {
      removeMediaStream(mediaStream);

      mediaStream.replaceMediaRecords(records);

      addMediaStream(mediaStream);
    }

    localMediaListeners.stream().forEach(l -> l.mediaUpdated(mediaStream));
  }

  private <D extends MediaDescriptor> void asyncEnrichMediaStream(MediaStream<D> mediaStream) {
    EXECUTOR.execute(() -> enrichMediaStream(mediaStream));
  }

  // Blocks
  private <D extends MediaDescriptor> void enrichMediaStream(MediaStream<D> mediaStream) {
    List<Exceptional<MediaRecord<D>>> results = incrementalIdentify(mediaStream);

    if(!results.isEmpty()) {
      StringBuilder builder = new StringBuilder();

      builder.append("Enrichment results for ").append(mediaStream).append(":");

      for(Exceptional<MediaRecord<D>> exceptional : results) {
        if(exceptional.isException()) {
          builder.append("\n - ").append(exceptional.getException());
        }
        else if(exceptional.isPresent()) {
          builder.append("\n - ").append(exceptional.get());
        }
        else {
          builder.append("\n - Unknown");
        }
      }

      LOGGER.info(builder.toString());
    }
  }

  private void addMediaStream(MediaStream<?> mediaStream) {
    synchronized(this) {
      streamsByPrintIdentifierByType.computeIfAbsent(mediaStream.getType(), k -> new HashMap<>()).put(mediaStream.getStreamPrint().getIdentifier(), mediaStream);
      mediaStream.getMediaRecords().keySet().stream().forEach(k -> streamsByIdentifier.computeIfAbsent(k, key -> new HashSet<>()).add(mediaStream));

      if(mediaStream.getParentUri() != null) {
        streamsByParentURI.computeIfAbsent(mediaStream.getParentUri(), k -> new HashSet<>()).add(mediaStream);
      }
    }
  }

  private <D extends MediaDescriptor> MediaStream<D> removeMediaStream(MediaStream<D> mediaStream) {
    synchronized(this) {
      @SuppressWarnings("unchecked")
      MediaStream<D> removedStream = (MediaStream<D>)streamsByPrintIdentifierByType.computeIfAbsent(mediaStream.getType(), k -> new HashMap<>()).remove(mediaStream.getStreamPrint().getIdentifier());

      if(removedStream != null) {
        removedStream.getMediaRecords().keySet().stream().forEach(k -> streamsByIdentifier.computeIfPresent(k, (key, v) -> v.remove(removedStream) && v.isEmpty() ? null : v));

        if(removedStream.getParentUri() != null) {
          streamsByParentURI.computeIfPresent(removedStream.getParentUri(), (k, v) -> v.remove(removedStream) && v.isEmpty() ? null : v);
        }
      }

      return removedStream;
    }
  }
}
