package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.IdentifierCollection;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.stream.BasicStream;
import hs.mediasystem.mediamanager.LocalMediaIdentificationService;
import hs.mediasystem.mediamanager.MediaIdentification;
import hs.mediasystem.mediamanager.StreamSource;
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
  private static final MediaType COLLECTION_MEDIA_TYPE = MediaType.of("COLLECTION");

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
          streamStore.findStream(streamId).ifPresent(stream -> {
            StreamSource source = streamStore.findStreamSource(streamId);

            key.earlyUnlock();

            MediaIdentification mediaIdentification = identificationService.identify(stream, source.getDataSourceNames());

            fetchAndStoreCollectionDescriptors(mediaIdentification);
            updateCacheWithIdentification(mediaIdentification, incremental, false);
            logEnrichmentResult(mediaIdentification, incremental);
          });
        }
      }
      finally {
        WORKLOAD.complete();
      }
    });
  }

  /**
   * Fetches all descriptors that are part of a collection, including descriptors that
   * may not be in the local collection, and stores them in the descriptor store for
   * fast access.
   *
   * @param mediaIdentification a {@link MediaIdentification} result to check for {@link Identifier}s of type COLLECTION.
   */
  private void fetchAndStoreCollectionDescriptors(MediaIdentification mediaIdentification) {
    // Production contains related Collection identifier which can be queried to get IdentifierCollection which in turn are each queried to get further Productions
    mediaIdentification.getResults().stream()
      .flatMap(Exceptional::ignoreAllAndStream)
      .map(t -> t.c)
      .map(Production.class::cast)
      .forEach(p -> p.getRelatedIdentifiers().stream()
        .filter(identifier -> identifier.getDataSource().getType().equals(COLLECTION_MEDIA_TYPE))  // After this filtering, stream consists of Collection type identifiers
        .filter(identifier -> identifier.getDataSource().getName().equals(p.getIdentifier().getDataSource().getName()))  // Only Collection type identifiers of same data source as production that contained it
        .forEach(this::fetchAndStoreCollectionItems)
      );
  }

  private void fetchAndStoreCollectionItems(Identifier collectionIdentifier) {
    identificationService.query(collectionIdentifier)
      .handle(RuntimeIOException.class, e -> LOGGER.warning("Exception while fetching collection descriptor for " + collectionIdentifier + ": " + Throwables.formatAsOneLine(e)))
      .map(IdentifierCollection.class::cast)
      .ifPresent(t -> {
        fetchAndStoreCollectionItems(t);
        descriptorStore.add(t);
      });
  }

  private void fetchAndStoreCollectionItems(IdentifierCollection identifierCollection) {
    for(Identifier identifier : identifierCollection.getItems()) {
      identificationService.query(identifier)
        .handle(RuntimeIOException.class, e -> LOGGER.warning("Exception while fetching descriptor for " + identifier + ": " + Throwables.formatAsOneLine(e)))
        .ifPresent(descriptorStore::add);
    }
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
                streamStore.put(scannerAndRootId, scannedStream);  // Adds as new or modify it
              }

              LOGGER.finer((existingStream == null ? "New stream found: " : "Existing stream modified: ") + scannedStream);

              asyncEnrichMediaStream(scannedStream.getId(), true);
            }
            else {  // Check if descriptor store contains the relevant descriptors
              for(Identifier identifier : streamStore.findIdentifications(existingStream.getId()).keySet()) {
                if(!identificationService.isQueryServiceAvailable(identifier.getDataSource())) {
                  continue;
                }

                MediaDescriptor mediaDescriptor = descriptorStore.find(identifier).orElse(null);

                if(mediaDescriptor == null) {
                  // One or more descriptors are missing, enrich:
                  LOGGER.warning("Existing stream is missing descriptors in cache (" + identifier + ") -> refetching: " + scannedStream);

                  asyncEnrichMediaStream(scannedStream.getId(), true);
                  break;
                }
                else if(mediaDescriptor instanceof Production) {
                  Production production = (Production)mediaDescriptor;
                  Identifier collectionIdentifier = production.getCollectionIdentifier().orElse(null);

                  if(collectionIdentifier != null) {
                    IdentifierCollection identifierCollection = (IdentifierCollection)descriptorStore.find(collectionIdentifier).orElse(null);

                    if(identifierCollection == null) {
                      LOGGER.warning("Existing stream is missing collection data in cache (" + collectionIdentifier + ") -> refetching: " + scannedStream);

                      asyncEnrichMediaStream(scannedStream.getId(), true);
                      break;
                    }

                    for(Identifier collectionItemIdentifier : identifierCollection.getItems()) {
                      if(descriptorStore.find(collectionItemIdentifier).orElse(null) == null) {
                        LOGGER.warning("Existing stream is missing collection items in cache (" + collectionItemIdentifier + " is missing, out of " + identifierCollection.getItems() + " from " + collectionIdentifier + ") -> refetching: " + scannedStream);

                        asyncEnrichMediaStream(scannedStream.getId(), true);
                        break;
                      }
                    }
                  }
                }
              }
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
      BasicStream stream = streamStore.findStream(streamId).orElse(null);

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

  private Map<Identifier, Tuple2<Match, MediaDescriptor>> findDescriptorsAndIdentifications(StreamID streamId) {
    Map<Identifier, Match> identifications = streamStore.findIdentifications(streamId);

    return identifications.entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getKey, e -> Tuple.of(e.getValue(), descriptorStore.find(e.getKey()).orElse(null))));
  }

  private Set<Tuple3<Identifier, Match, MediaDescriptor>> createMergedRecords(BasicStream stream, Set<Tuple3<Identifier, Match, MediaDescriptor>> records) {
    Map<Identifier, Tuple2<Match, MediaDescriptor>> originalRecords = findDescriptorsAndIdentifications(stream.getId());

    for(Tuple3<Identifier, Match, MediaDescriptor> record : records) {
      Tuple2<Match, MediaDescriptor> original = originalRecords.get(record.a);

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
      Set<Tuple3<Identifier, Match, MediaDescriptor>> records =
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

    for(Exceptional<Tuple3<Identifier, Match, MediaDescriptor>> exceptional : mediaIdentification.getResults()) {
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
