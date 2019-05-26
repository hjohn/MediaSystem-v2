package hs.mediasystem.db;

import hs.mediasystem.mediamanager.LocalMediaManager;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.NamedThreadFactory;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.bg.BackgroundTaskRegistry;
import hs.mediasystem.util.bg.BackgroundTaskRegistry.Workload;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaManagerUpdater {
  private static final Logger LOGGER = Logger.getLogger(MediaManagerUpdater.class.getName());
  private static final LinkedBlockingQueue<Runnable> QUEUE = new LinkedBlockingQueue<>();
  private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, QUEUE, new NamedThreadFactory("MediaManagerUpdater", Thread.NORM_PRIORITY - 2, true));
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Downloading Metadata");

  @Inject private LocalMediaManager localMediaManager;
  @Inject private DatabaseStreamStore streamStore;

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
        Thread.sleep(10000);
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
    reidentifyThread.setName("MediaManagerUpdater-Reidentifier");
    reidentifyThread.start();
  }

  private void asyncEnrichMediaStream(StreamID streamId, boolean incremental) {
    WORKLOAD.start();
    EXECUTOR.execute(() -> {
      try {
        if(incremental) {
          localMediaManager.incrementallyUpdateStream(streamId);
        }
        else {
          localMediaManager.updateStream(streamId);
        }

        streamStore.markEnriched(streamId);
      }
      finally {
        WORKLOAD.complete();
      }
    });
  }

  public synchronized void update(long scannerId, List<Exceptional<List<BasicStream>>> rootResults) {
    for(int rootResultIdx = 0; rootResultIdx < rootResults.size(); rootResultIdx++) {
      Exceptional<List<BasicStream>> rootResult = rootResults.get(rootResultIdx);

      if(rootResult.isPresent()) {
        int scannerAndRootId = (int)scannerId + rootResultIdx * 65536;
        Map<StreamID, BasicStream> existingStreams = streamStore.findByScannerId(scannerAndRootId);  // Returns all active streams (not deleted)

        for(BasicStream scannedStream : rootResult.get()) {
          try {
            BasicStream existingStream = existingStreams.remove(scannedStream.getId());

            if(existingStream == null) {
              streamStore.add(scannerAndRootId, scannedStream);  // Adds as new

              LOGGER.finer("New stream found: " + scannedStream);

              asyncEnrichMediaStream(scannedStream.getId(), true);
            }
            else if(!scannedStream.equals(existingStream)) {
              streamStore.add(scannerAndRootId, scannedStream);  // Modify it

              LOGGER.finer("Existing stream modified: " + scannedStream);

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

        existingStreams.entrySet().stream()
          .peek(e -> LOGGER.finer("Existing Stream deleted: " + e.getValue()))
          .map(Map.Entry::getKey)
          .forEach(streamStore::remove);
      }
    }
  }
}
