package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService.Result;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.AutoReentrantLock;
import hs.mediasystem.util.AutoReentrantLock.Key;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.Tuple;
import hs.mediasystem.util.Tuple.Tuple2;
import hs.mediasystem.util.Tuple.Tuple3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalMediaManager {
  private static final Logger LOGGER = Logger.getLogger(LocalMediaManager.class.getName());

  @Inject private List<IdentificationService> identificationServices;
  @Inject private List<QueryService<?>> queryServices;
  @Inject private StreamStore store;  // Modifications require synchronization

  private final Map<DataSource, IdentificationService> identificationServicesByDataSource = new HashMap<>();
  private final Map<DataSource, QueryService<MediaDescriptor>> queryServicesByDataSource = new HashMap<>();
  private final List<LocalMediaListener> localMediaListeners = new ArrayList<>();
  private final AutoReentrantLock storeConsistencyLock = new AutoReentrantLock();

  @PostConstruct
  private void postConstruct() {
    LOGGER.info("Instantiated with " + identificationServices.size() + " identification services and " + queryServices.size() + " query services");

    for(IdentificationService service : identificationServices) {
      if(identificationServicesByDataSource.put(service.getDataSource(), service) != null) {
        LOGGER.warning("Multiple identification services available for datasource: " + service.getDataSource());
      }
    }
    for(QueryService<?> service : queryServices) {
      @SuppressWarnings("unchecked")
      QueryService<MediaDescriptor> castedService = (QueryService<MediaDescriptor>)service;

      if(queryServicesByDataSource.put(service.getDataSource(), castedService) != null) {
        LOGGER.warning("Multiple query services available for datasource: " + service.getDataSource());
      }
    }
  }

  public void put(BasicStream stream, StreamSource source, Set<Tuple3<Identifier, Identification, MediaDescriptor>> descriptors) {
    try(Key key = storeConsistencyLock.lock()) {
      store.put(stream, source, descriptors);
    }
  }

  public void remove(BasicStream stream) {
    try(Key key = storeConsistencyLock.lock()) {
      store.remove(stream);
    }
  }

  public void addListener(LocalMediaListener listener) {
    localMediaListeners.add(listener);
  }

  public void removeListener(LocalMediaListener listener) {
    localMediaListeners.remove(listener);
  }

  public MediaIdentification incrementallyUpdateStream(StreamID streamId) {
    return enrichMediaStream(streamId, Mode.INCREMENTAL);
  }

  public MediaIdentification updateStream(StreamID streamId) {
    return enrichMediaStream(streamId, Mode.UPDATE);
  }

  public MediaIdentification reidentifyStream(StreamID streamId) {
    return enrichMediaStream(streamId, Mode.REPLACE);
  }

  // Method may block
  private MediaIdentification identify(StreamID streamId, boolean incremental) {
    try(Key key = storeConsistencyLock.lock()) {
      BasicStream stream = store.findStream(streamId);

      if(stream == null) {
        throw new IllegalArgumentException("No matching stream found for streamId: " + streamId);
      }

      Map<Identifier, Tuple2<Identification, MediaDescriptor>> records = store.findDescriptorsAndIdentifications(stream.getId());
      MediaType type = stream.getType();

      // Get old identifications with missing descriptors (queries), but skip if there is no matching query service anyway:
      List<Tuple2<Identifier, Identification>> identificationsMissingQueries = !incremental ? Collections.emptyList() : records.entrySet().stream()
        .filter(e -> e.getValue().b == null)
        .filter(e -> queryServicesByDataSource.get(e.getKey().getDataSource()) != null)
        .map(e -> Tuple.of(e.getKey(), e.getValue().a))
        .collect(Collectors.toList());

      // Create list of already identified datasources (only filled if incremental):
      Set<DataSource> identifiedDataSources = !incremental ? new HashSet<>() : records.keySet().stream()
        .map(Identifier::getDataSource)
        .collect(Collectors.toCollection(HashSet::new));

      // Create list of identifications to perform:
      List<IdentificationService> identificationsToPerform = identificationServicesByDataSource.entrySet().stream()
        .filter(e -> e.getKey().getType().equals(type))
        .filter(e -> !identifiedDataSources.contains(e.getKey()))  // Don't identify ones that exist already (if incremental true)
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());

      key.earlyUnlock();  // Early unlock because next method is really slow

      return performIdentificationCalls(stream, identificationsMissingQueries, identifiedDataSources, identificationsToPerform);
    }
  }

  private MediaIdentification performIdentificationCalls(
    BasicStream stream,
    List<Tuple2<Identifier, Identification>> identificationsMissingQueries,   // Descriptor query calls to do
    Set<DataSource> identifiedDataSources,                                    // Sources to skip (if incremental)
    List<IdentificationService> identificationsToPerform                      // Identify calls to do
  ) {
    // Get new identifications:
    List<Exceptional<Tuple2<Identifier, Identification>>> newIdentifications = identificationsToPerform.stream()
      .map(identificationService -> Exceptional.from(() ->
        Optional.ofNullable(
          identificationService.identify(stream)
        ).orElseThrow(() -> new UnknownStreamException(stream, identificationService))
      ))
      .collect(Collectors.toList());

    List<Exceptional<Tuple3<Identifier, Identification, Result<MediaDescriptor>>>> results = new ArrayList<>();

    // Concat new identifications and existing ones with missing data into new list:
    List<Exceptional<Tuple2<Identifier, Identification>>> identificationsToQuery = Stream.of(
      newIdentifications,
      identificationsMissingQueries.stream().map(Exceptional::of).collect(Collectors.toList())
    )
    .flatMap(Collection::stream).collect(Collectors.toList());

    // Loop here as queries may return new Identifications that in turn require querying:
    while(!identificationsToQuery.isEmpty()) {
      identificationsToQuery
        .forEach(identificationExceptional -> results.add(
          identificationExceptional
            .map((Tuple2<Identifier, Identification> t) -> queryServicesByDataSource.get(t.a.getDataSource()))
            .map(qs -> qs.query(identificationExceptional.get().a))  // get() is safe here, won't get here if not present
            .map(r -> Tuple.of(identificationExceptional.get().a, identificationExceptional.get().b, r))
            .or(() -> Exceptional.of(Tuple.of(identificationExceptional.get().a, identificationExceptional.get().b, null)))
      ));

      // Update identifiedDataSources:
      identificationsToQuery.forEach(exI -> exI.map(t -> t.a)
        .map(Identifier::getDataSource)
        .ignore(Throwable.class)
        .ifPresent(identifiedDataSources::add)
      );

      // Check Results for new Identifications:
      identificationsToQuery = results.stream()
        .map(e -> e.map(t -> t.c))
        .flatMap(Exceptional::ignoreAllAndStream)
        .map(Result::getNewIdentifications)
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .map(e -> Tuple.of(e.getKey(), e.getValue()))
        .filter(t -> !identifiedDataSources.contains(t.a.getDataSource()))  // Don't identify ones that exist already (prevent id loops)
        .map(Exceptional::of)
        .collect(Collectors.toList());
    }

    return new MediaIdentification(
      stream,
      results.stream()
        .map(e -> e.map(t -> Tuple.of(t.a, t.b, t.c == null ? null : t.c.getMediaDescriptor())))
        .collect(Collectors.toSet())
    );
  }

  private Set<Tuple3<Identifier, Identification, MediaDescriptor>> createMergedRecords(BasicStream stream, Set<Tuple3<Identifier, Identification, MediaDescriptor>> records) {
    Map<Identifier, Tuple2<Identification, MediaDescriptor>> originalRecords = store.findDescriptorsAndIdentifications(stream.getId());

    for(Tuple3<Identifier, Identification, MediaDescriptor> record : records) {
      Tuple2<Identification, MediaDescriptor> original = originalRecords.get(record.a);

      if(original == null || original.b == null || record.c != null) {
        originalRecords.put(record.a, Tuple.of(record.b, record.c));
      }
    }

    return originalRecords.entrySet().stream()
      .map(e -> Tuple.of(e.getKey(), e.getValue().a, e.getValue().b))
      .collect(Collectors.toSet());
  }

  private enum Mode {

    /**
     * Identifies missing provides only, and fetches missing descriptors only.
     */
    INCREMENTAL,

    /**
     * Reidentifies everything, fetches everything, but does an incremental merge if there were any exceptions otherwise does a replace.
     */
    UPDATE,

    /**
     * Reidentifies everything, and discards any previous descriptors and replaces them with the newly found ones.
     */
    REPLACE;
  }

  // Blocks
  private MediaIdentification enrichMediaStream(StreamID streamId, Mode mode) {
    MediaIdentification mediaIdentification = identify(streamId, mode == Mode.INCREMENTAL);
    BasicStream stream = mediaIdentification.getStream();

    try(Key key = storeConsistencyLock.lock()) {
      boolean hasExceptions = mediaIdentification.getResults().stream().filter(Exceptional::isException).findAny().isPresent();

      Set<Tuple3<Identifier, Identification, MediaDescriptor>> records =
        mode == Mode.REPLACE || (mode == Mode.UPDATE && !hasExceptions) ?
          mediaIdentification.getResults().stream().flatMap(Exceptional::ignoreAllAndStream).collect(Collectors.toSet()) :
          createMergedRecords(mediaIdentification.getStream(), mediaIdentification.getResults().stream().flatMap(Exceptional::ignoreAllAndStream).collect(Collectors.toSet()));

      store.update(stream, records);

      key.earlyUnlock();

      localMediaListeners.stream().forEach(l -> l.mediaUpdated(stream, records));

      logEnrichmentResult(mode, mediaIdentification);

      return mediaIdentification;
    }
  }

  private static void logEnrichmentResult(Mode mode, MediaIdentification mediaIdentification) {
    StringBuilder builder = new StringBuilder();
    boolean warning = false;

    builder.append("Enrichment " + mode + " results for ").append(mediaIdentification.getStream()).append(":");

    for(Exceptional<Tuple3<Identifier, Identification, MediaDescriptor>> exceptional : mediaIdentification.getResults()) {
      if(exceptional.isException()) {
        builder.append("\n - ").append(Throwables.formatAsOneLine(exceptional.getException()));
        warning = true;
      }
      else if(exceptional.isPresent()) {
        builder.append("\n - ").append(exceptional.get());
      }
      else {
        warning = true;
        builder.append("\n - Unknown");
      }
    }

    if(warning) {
      LOGGER.warning(builder.toString());
    }
    else {
      LOGGER.info(builder.toString());
    }
  }
}
