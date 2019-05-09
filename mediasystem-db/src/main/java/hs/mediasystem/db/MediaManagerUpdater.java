package hs.mediasystem.db;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.MediaRecord;
import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.mediamanager.LocalMediaListener;
import hs.mediasystem.mediamanager.LocalMediaManager;
import hs.mediasystem.mediamanager.StreamTags;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.Guard;
import hs.mediasystem.util.NamedThreadFactory;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.Tuple;
import hs.mediasystem.util.Tuple.Tuple3;
import hs.mediasystem.util.bg.BackgroundTaskRegistry;
import hs.mediasystem.util.bg.BackgroundTaskRegistry.Workload;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaManagerUpdater {
  private static final Logger LOGGER = Logger.getLogger(MediaManagerUpdater.class.getName());
  private static final LinkedBlockingQueue<Runnable> QUEUE = new LinkedBlockingQueue<>();
  private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, QUEUE, new NamedThreadFactory("MediaManagerUpdater", Thread.NORM_PRIORITY - 2, true));
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Downloading Metadata");

  @Inject private LocalMediaManager localMediaManager;
  @Inject private DatabaseLocalMediaStore mediaStore;
  @Inject private LocalMediaCodec localMediaCodec;

  private final Guard<Map<StreamID, EnrichTimes>> enrichTimes = new Guard<>(new HashMap<>());

  public void setScannerIds(Map<Integer, StreamTags> scannerIds) {
    // Can only be called once for now
    // Should purge database of scanner id's not present
    // Probably best to remove scanner id's altogether, and just grab cached data from db
    // based on the configured scan paths (which is a prefix of the path stored) and scanner type

    initializeMediaManager(scannerIds);
    initializeUpdateListener();
    triggerInitialEnriches();
    initializePeriodicEnrichThread();
  }

  private void initializeMediaManager(Map<Integer, StreamTags> scannerIds) {
    for(LocalMedia localMedia : mediaStore.findAllActive()) {
      int key = (int)localMedia.getScannerId() & 0xffff;

      if(scannerIds.containsKey(key)) {
        MediaStream mediaStream = localMediaCodec.toMediaStream(localMedia);

        if(mediaStream != null) {
          localMediaManager.put(
            mediaStream.getStream(),
            scannerIds.get(key),
            mediaStream.getMediaRecords().entrySet().stream()
              .map(e -> Tuple.of(e.getKey(), e.getValue().getIdentification(), e.getValue().getMediaDescriptor()))
              .collect(Collectors.toSet())
          );

          enrichTimes.write(et -> et.put(mediaStream.getStream().getId(), new EnrichTimes(mediaStream.getLastEnrichTime(), mediaStream.getNextEnrichTime())));
        }
      }
    }
  }

  private void initializeUpdateListener() {
    localMediaManager.addListener(new LocalMediaListener() {
      @Override
      public <D extends MediaDescriptor> void mediaUpdated(BasicStream stream, Set<Tuple3<Identifier, Identification, D>> records) {
        MediaManagerUpdater.this.mediaUpdated(stream, records);
      }
    });
  }

  private void triggerInitialEnriches() {
    enrichTimes.execute(et -> et.entrySet().stream()
      .filter(e -> e.getValue().lastEnrichTime == null)
      .forEach(e -> asyncEnrichMediaStream(e.getKey(), true))
    );
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
          Instant now = Instant.now();

          enrichTimes.execute(et -> et.entrySet().stream()
            .filter(e -> e.getValue().nextEnrichTime != null && e.getValue().nextEnrichTime.isBefore(now))
            .map(Map.Entry::getKey)
            .limit(40)
            .forEach(sid -> asyncEnrichMediaStream(sid, false))
          );
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

    /*
     * TODO Donot let LocalMediaManager make changes on its own.
     *
     * Instead, have it return the results of an update (MediaIdentification), merge
     * it here with MediaStream, and then modify LocalMediaManager by putting that
     * MediaStream.
     *
     * This would remove the need for a listener, and would make it possible to
     * cancel existing enrich tasks by not entering the results to LocalMediaManager
     * when they're cancelled.
     *
     * The reidentify method would need to move to this class, as without a listener
     * this class wouldn't get informed of the changes made by the externally
     * triggered reidentify.
     */

    WORKLOAD.start();
    EXECUTOR.execute(() -> {
      try {
        if(incremental) {
          localMediaManager.incrementallyUpdateStream(streamId);
        }
        else {
          localMediaManager.updateStream(streamId);
        }
      }
      finally {
        WORKLOAD.complete();
      }
    });
  }

  private <D extends MediaDescriptor> void mediaUpdated(BasicStream stream, Set<Tuple3<Identifier, Identification, D>> records) {
    updateEnrichTime(stream.getId());

    EnrichTimes times = enrichTimes.read(et -> et.get(stream.getId()));

    MediaStream mediaStream = new MediaStream(
      stream,
      times.lastEnrichTime,
      times.nextEnrichTime,
      records.stream()
        .collect(Collectors.toMap(t -> t.a, t -> new MediaRecord(t.a, t.b, t.c)))
    );

    LocalMedia localMedia = localMediaCodec.toLocalMedia(1, null, mediaStream);
    LocalMedia existingLocalMedia = mediaStore.findById(mediaStream.getStream().getId().asInt());

    if(existingLocalMedia != null) {
      localMedia.setScannerId(existingLocalMedia.getScannerId());
      mediaStore.store(localMedia);
    }
    else {
      LOGGER.warning("Media updated that did not exist in db: " + mediaStream);
    }
  }

  private void updateEnrichTime(StreamID streamId) {
    enrichTimes.write(et -> {
      EnrichTimes times = et.get(streamId);
      Instant lastEnrichTime = times.lastEnrichTime;
      Instant now = Instant.now();
      Instant nextEnrichTime;

      if(lastEnrichTime != null) {
        double secondsUntilNextTime = Math.abs(Duration.between(lastEnrichTime, now).toSeconds()) + 1;  // Make sure this is not zero or negative, or an infinite loop can result below

        while(secondsUntilNextTime < 6 * 60 * 60) {
          secondsUntilNextTime *= (0.1 * Math.random() + 1.4);
        }

        while(secondsUntilNextTime > 90 * 24 * 60 * 60L) {
          secondsUntilNextTime *= 0.99;
        }

        nextEnrichTime = now.plusSeconds((long)secondsUntilNextTime);
      }
      else {
        nextEnrichTime = now.plusSeconds(6 * 60 * 60);
      }

      et.put(streamId, new EnrichTimes(now, nextEnrichTime));
    });
  }

  public synchronized void update(long scannerId, StreamTags tags, List<Exceptional<List<BasicStream>>> rootResults) {
    for(int rootResultIdx = 0; rootResultIdx < rootResults.size(); rootResultIdx++) {
      Exceptional<List<BasicStream>> rootResult = rootResults.get(rootResultIdx);

      if(rootResult.isPresent()) {
        long scannerAndRootId = scannerId + rootResultIdx * 65536;
        Map<Integer, LocalMedia> existingLocalMedias = mediaStore.findByScannerId(scannerAndRootId);

        for(BasicStream scannedStream : rootResult.get()) {
          MediaStream mediaStream = new MediaStream(scannedStream, null, null, Collections.emptyMap());

          try {
            LocalMedia existingLocalMedia = existingLocalMedias.remove(scannedStream.getId().asInt());

            if(existingLocalMedia == null) {
              putMediaStream(scannerAndRootId, tags, mediaStream, "New Stream found: " + scannedStream);
            }
            else {
              MediaStream existingMediaStream = localMediaCodec.toMediaStream(existingLocalMedia);

              if(existingMediaStream == null) {
                putMediaStream(scannerAndRootId, tags, mediaStream, "Error decoding existing Stream, replacing with new version: " + existingLocalMedia);
              }
              else if(existingLocalMedia.getDeleteTime() != null) {
                putMediaStream(scannerAndRootId, tags, existingMediaStream, "Existing Stream undeleted: " + existingMediaStream);
              }
              else if(!scannedStream.equals(existingMediaStream.getStream())) {
                putMediaStream(scannerAndRootId, tags, mediaStream, "Existing stream modified: " + scannedStream);
              }
            }
          }
          catch(Throwable t) {
            LOGGER.severe("Exception while updating LocalMediaManager with: " + scannedStream + ": " + Throwables.formatAsOneLine(t));
          }
        }

        /*
         * After updating LocalMediaManager, existingStreams will only contain the streams that were
         * not found during the last scan.  These will be marked deleted.
         */

        // TODO maybe all changes to LocalMediaManager should be transactional. Also to database (in case of restart in middle of this process)

        for(LocalMedia deletedMedia : existingLocalMedias.values()) {
          if(deletedMedia.getDeleteTime() == null) {
            deleteMediaStream(deletedMedia, "Existing Stream deleted: " + deletedMedia);
          }
          // TODO else, if delete time very old, permanently delete
        }
      }
    }
  }

  private void deleteMediaStream(LocalMedia deletedMedia, String message) {
    LOGGER.finer(message);

    deletedMedia.setDeleteTime(LocalDateTime.now());

    mediaStore.store(deletedMedia);

    MediaStream mediaStream = localMediaCodec.toMediaStream(deletedMedia);

    if(mediaStream != null) {
      enrichTimes.write(et -> {
        localMediaManager.remove(mediaStream.getStream());
        et.remove(mediaStream.getStream().getId());
      });
    }
  }

  private void putMediaStream(long scannerId, StreamTags tags, MediaStream mediaStream, String message) {
    LOGGER.finer(message);

    mediaStore.store(localMediaCodec.toLocalMedia(scannerId, null, mediaStream));

    enrichTimes.write(et -> {
      localMediaManager.put(
        mediaStream.getStream(),
        tags,
        mediaStream.getMediaRecords().entrySet().stream()
          .map(e -> Tuple.of(e.getKey(), e.getValue().getIdentification(), e.getValue().getMediaDescriptor()))
          .collect(Collectors.toSet())
      );
      asyncEnrichMediaStream(mediaStream.getStream().getId(), true);
      et.put(mediaStream.getStream().getId(), new EnrichTimes(mediaStream.getLastEnrichTime(), mediaStream.getNextEnrichTime()));
    });
  }

//  private static String hashAttributes(Attributes attributes) {
//    try {
//      MessageDigest digest = MessageDigest.getInstance("SHA-256");
//
//      attributes.keySet().stream()
//        .sorted()
//        .forEach(k -> {
//          digest.update(k.getBytes(StandardCharsets.UTF_8));
//          digest.update(attributes.get(k).toString().getBytes(StandardCharsets.UTF_8));
//        });
//
//      return CryptoUtil.toHex(digest.digest());
//    }
//    catch(NoSuchAlgorithmException e) {
//      throw new IllegalStateException(e);
//    }
//  }

  private static class EnrichTimes {
    final Instant lastEnrichTime;
    final Instant nextEnrichTime;

    EnrichTimes(Instant lastEnrichTime, Instant nextEnrichTime) {
      this.lastEnrichTime = lastEnrichTime;
      this.nextEnrichTime = nextEnrichTime;
    }
  }
}
