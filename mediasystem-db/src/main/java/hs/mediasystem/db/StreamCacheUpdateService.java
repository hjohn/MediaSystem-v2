package hs.mediasystem.db;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.IdentifierCollection;
import hs.mediasystem.ext.basicmediatypes.services.QueryService.Result;
import hs.mediasystem.mediamanager.LocalMediaIdentificationService;
import hs.mediasystem.mediamanager.MediaIdentification;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.AutoReentrantLock;
import hs.mediasystem.util.AutoReentrantLock.Key;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.NamedThreadFactory;
import hs.mediasystem.util.RuntimeIOException;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.Tuple;
import hs.mediasystem.util.Tuple.Tuple2;
import hs.mediasystem.util.Tuple.Tuple3;
import hs.mediasystem.util.bg.BackgroundTaskRegistry;
import hs.mediasystem.util.bg.BackgroundTaskRegistry.Workload;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamCacheUpdateService {
  private static final Logger LOGGER = Logger.getLogger(StreamCacheUpdateService.class.getName());
  private static final LinkedBlockingQueue<Runnable> QUEUE = new LinkedBlockingQueue<>();
  private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, QUEUE, new NamedThreadFactory("StreamCacheUpdateSvc", Thread.NORM_PRIORITY - 2, true));
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Downloading Metadata");

  @Inject private LocalMediaIdentificationService identificationService;
  @Inject private DatabaseStreamStore streamStore;
  @Inject private DatabaseDescriptorStore descriptorStore;

  private final AutoReentrantLock storeConsistencyLock = new AutoReentrantLock();  // Used to sync actions of this class

  @PostConstruct
  private void postConstruct() {
    triggerInitialEnriches();
    initializePeriodicEnrichThread();
  }

  private void triggerInitialEnriches() {
    Set<StreamID> unenrichedStreams = streamStore.findUnenrichedStreams();

    LOGGER.fine("Triggering first time enrich of " + unenrichedStreams.size() + " streams");

    unenrichedStreams.forEach(sid -> asyncEnrichMediaStream(sid, true));
  }

  private void initializePeriodicEnrichThread() {
    Thread reidentifyThread = new Thread(() -> {
      try {
        Thread.sleep(300000);  // Initial delay, to avoid triggering immediately on restart
      }
      catch(InterruptedException e1) {
        // Ignore
      }

      for(;;) {
        if(EXECUTOR.getQueue().isEmpty()) {
          streamStore.findStreamsNeedingRefresh(40).stream()
            .forEach(sid -> asyncEnrichMediaStream(sid, false));
        }

        try {
          Thread.sleep(300000);
        }
        catch(InterruptedException e) {
          // Ignore
        }
      }
    });

    reidentifyThread.setDaemon(true);
    reidentifyThread.setPriority(Thread.NORM_PRIORITY - 2);
    reidentifyThread.setName("StreamCacheUpdateService-Reidentifier");
    reidentifyThread.start();
  }

  private void asyncEnrichMediaStream(StreamID streamId, boolean incremental) {
    WORKLOAD.start();
    EXECUTOR.execute(() -> {
      try {
        try(Key key = storeConsistencyLock.lock()) {
          BasicStream stream = streamStore.findStream(streamId);

          if(stream == null) {
            return;
          }

          StreamSource source = streamStore.findStreamSource(streamId);

          key.earlyUnlock();

          MediaIdentification mediaIdentification = identificationService.identify(stream, source.getDataSourceNames());

          fetchAndStoreCollectionDescriptors(mediaIdentification);
          updateCacheWithIdentification(mediaIdentification, incremental, false);
          logEnrichmentResult(mediaIdentification, incremental);
        }
      }
      finally {
        WORKLOAD.complete();
      }
    });
  }

  private void fetchAndStoreCollectionDescriptors(MediaIdentification mediaIdentification) {
    mediaIdentification.getResults().stream()
      .flatMap(Exceptional::ignoreAllAndStream)
      .filter(t -> t.a.getDataSource().getType().equals(MediaType.of("COLLECTION")))
      .map(t -> t.c)
      .map(IdentifierCollection.class::cast)
      .map(IdentifierCollection::getItems)
      .flatMap(Collection::stream)
      .filter(identifier -> descriptorStore.get(identifier) == null)
      .forEach(identifier -> {
        identificationService.query(identifier)
          .handle(RuntimeIOException.class, e -> LOGGER.warning("Exception while fetching descriptor for " + identifier + ": " + Throwables.formatAsOneLine(e)))
          .map(Result::getMediaDescriptor)
          .ifPresent(descriptorStore::add);
      });
  }

  public synchronized void update(long importSourceId, List<Exceptional<List<BasicStream>>> rootResults) {
    for(int rootResultIdx = 0; rootResultIdx < rootResults.size(); rootResultIdx++) {
      Exceptional<List<BasicStream>> rootResult = rootResults.get(rootResultIdx);

      if(rootResult.isPresent()) {
        int scannerAndRootId = (int)importSourceId + rootResultIdx * 65536;
        Map<StreamID, BasicStream> existingStreams = streamStore.findByImportSourceId(scannerAndRootId);  // Returns all active streams (not deleted)

        for(BasicStream scannedStream : rootResult.get()) {
          try {
            BasicStream existingStream = existingStreams.remove(scannedStream.getId());

            if(existingStream == null || !scannedStream.equals(existingStream)) {
              try(Key key = storeConsistencyLock.lock()) {
                streamStore.add(scannerAndRootId, scannedStream);  // Adds as new or modify it
              }

              LOGGER.finer((existingStream == null ? "New stream found: " : "Existing stream modified: ") + scannedStream);

              asyncEnrichMediaStream(scannedStream.getId(), true);
            }
          }
          catch(Throwable t) {
            LOGGER.severe("Exception while updating: " + scannedStream + ": " + Throwables.formatAsOneLine(t));
          }
        }

        /*
         * After updating, existingStreams will only contain the streams that were
         * not found during the last scan.  These will be removed.
         */

        try(Key key = storeConsistencyLock.lock()) {
          existingStreams.entrySet().stream()
            .peek(e -> LOGGER.finer("Existing Stream deleted: " + e.getValue()))
            .map(Map.Entry::getKey)
            .forEach(streamStore::remove);
        }
      }
    }
  }

  public MediaIdentification reidentifyStream(StreamID streamId) {
    try(Key key = storeConsistencyLock.lock()) {
      BasicStream stream = streamStore.findStream(streamId);

      if(stream == null) {
        return null;
      }

      StreamSource source = streamStore.findStreamSource(streamId);

      key.earlyUnlock();

      MediaIdentification mediaIdentification = identificationService.identify(stream, source.getDataSourceNames());

      fetchAndStoreCollectionDescriptors(mediaIdentification);
      updateCacheWithIdentification(mediaIdentification, false, true);
      logEnrichmentResult(mediaIdentification, false);

      return mediaIdentification;
    }
  }

  private Map<Identifier, Tuple2<Identification, MediaDescriptor>> findDescriptorsAndIdentifications(StreamID streamId) {
    Map<Identifier, Identification> identifications = streamStore.findIdentifications(streamId);

    return identifications.entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getKey, e -> Tuple.of(e.getValue(), descriptorStore.get(e.getKey()))));
  }

  private Set<Tuple3<Identifier, Identification, MediaDescriptor>> createMergedRecords(BasicStream stream, Set<Tuple3<Identifier, Identification, MediaDescriptor>> records) {
    Map<Identifier, Tuple2<Identification, MediaDescriptor>> originalRecords = findDescriptorsAndIdentifications(stream.getId());

    for(Tuple3<Identifier, Identification, MediaDescriptor> record : records) {
      Tuple2<Identification, MediaDescriptor> original = originalRecords.get(record.a);

      if(original == null || original.b == null || record.c != null) {
        Identifier identifier = record.a;

        // Only one record should be present per datasource, so remove corresponding one from original records if a new record was supplied from the same datasource:
        originalRecords.keySet().removeIf(i -> i.getDataSource().equals(identifier.getDataSource()));
        originalRecords.put(record.a, Tuple.of(record.b, record.c));
      }
    }

    return originalRecords.entrySet().stream()
      .map(e -> Tuple.of(e.getKey(), e.getValue().a, e.getValue().b))
      .collect(Collectors.toSet());
  }

  private void updateCacheWithIdentification(MediaIdentification mediaIdentification, boolean incremental, boolean replace) {
    try(Key key = storeConsistencyLock.lock()) {
      StreamID streamId = mediaIdentification.getStream().getId();
      boolean hasExceptions = mediaIdentification.getResults().stream().filter(Exceptional::isException).findAny().isPresent();

      // Create final records to store:
      Set<Tuple3<Identifier, Identification, MediaDescriptor>> records =
        replace || (!incremental && !hasExceptions) ?
          mediaIdentification.getResults().stream().flatMap(Exceptional::ignoreAllAndStream).collect(Collectors.toSet()) :
          createMergedRecords(mediaIdentification.getStream(), mediaIdentification.getResults().stream().flatMap(Exceptional::ignoreAllAndStream).collect(Collectors.toSet()));

      // Store identifiers with stream:
      streamStore.putIdentifications(streamId, records.stream().collect(Collectors.toMap(t -> t.a, t -> t.b)));

      // Store descriptors in descriptor store:
      records.stream()
        .map(r -> r.c)
        .filter(Objects::nonNull)
        .forEach(descriptorStore::add);

      // Mark enriched
      streamStore.markEnriched(streamId);
    }
  }

  private static void logEnrichmentResult(MediaIdentification mediaIdentification, boolean incremental) {
    StringBuilder builder = new StringBuilder();
    boolean warning = false;

    builder.append("Enrichment results " + (incremental ? "(incremental) " : "") + "for ").append(mediaIdentification.getStream()).append(":");

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
