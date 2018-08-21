package hs.mediasystem.db;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import hs.mediasystem.ext.basicmediatypes.scan.MediaStream;
import hs.mediasystem.mediamanager.LocalMediaListener;
import hs.mediasystem.mediamanager.LocalMediaManager;
import hs.mediasystem.util.AutoReentrantLock;
import hs.mediasystem.util.NamedThreadFactory;
import hs.mediasystem.util.Throwables;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ScannerController {
  private static final ScheduledThreadPoolExecutor EXECUTOR;
  private static final Logger LOGGER = Logger.getLogger(ScannerController.class.getName());
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
    .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
    .registerModule(new ParameterNamesModule(Mode.PROPERTIES))
    .registerModule(new JavaTimeModule())
    .registerModule(new RecordGroupModule());

  static {
    EXECUTOR = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("ScannerController"));
    EXECUTOR.setRemoveOnCancelPolicy(true);
  }

  @Inject private LocalMediaManager localMediaManager;
  @Inject private DatabaseLocalMediaStore mediaStore;

  private final AutoReentrantLock mediaManagerLock = new AutoReentrantLock();

  private AtomicBoolean cancelled;
  private ScheduledFuture<?> future;

  @PostConstruct
  private void postConstruct() {
    for(LocalMedia localMedia : mediaStore.findAllActive()) {
      MediaStream<?> mediaStream = toMediaStream(localMedia);

      if(mediaStream != null) {
        localMediaManager.add(mediaStream);
      }
    }

    localMediaManager.addListener(new LocalMediaListener() {

      @Override
      public void mediaAdded(MediaStream<?> mediaStream) {
      }

      @Override
      public void mediaUpdated(MediaStream<?> mediaStream) {
        System.err.println(">>> MediaUpdated: " + mediaStream);

        LocalMedia localMedia = toLocalMedia(1, null, mediaStream);
        LocalMedia existingLocalMedia = mediaStore.findById(mediaStream.getStreamPrint().getIdentifier());

        if(existingLocalMedia != null) {
          localMedia.setScannerId(existingLocalMedia.getScannerId());
        }

        mediaStore.store(localMedia);
      }

      @Override
      public void mediaRemoved(MediaStream<?> mediaStream) {
      }
    });
  }

  // Run scans periodically, taking care to not overlap scans
  // Scan results should be added to db

  public void setSuppliers(Map<Long, Supplier<List<? extends MediaStream<?>>>> suppliers) {
    if(future != null) {
      cancelled.set(true);
      future.cancel(true);
    }

    // TODO when scanners are reconfigured, we may need to purge things from MediaManager...

    Map<Long, Supplier<List<? extends MediaStream<?>>>> copiedSuppliers = new HashMap<>(suppliers);

    try(AutoReentrantLock lock = mediaManagerLock.lock()) {  // Grabbing the lock will ensure that any old tasks aren't midway in an update and have been properly cancelled
      cancelled = new AtomicBoolean();
      future = EXECUTOR.scheduleWithFixedDelay(() -> scanAll(copiedSuppliers, cancelled), 0, 5, TimeUnit.MINUTES);
    }
  }

  // TODO When LocalMediaManager identifies / enriches objects, those results must be stored in the DB, so a listener needs to be added for these events
  // TODO If MediaStream does not contain deleteTime/scannerId then we need to re-query the db to get those before we can do an update based on only a MediaStream object (as only LocalMedia object contains those mutable fields).

  /**
   * Executes scans for all the given Scanners.  If cancelled, exit early and donot report any results.
   *
   * @param suppliers a list of {@link Supplier}s
   * @param cancelled whether or not this task was cancelled
   */
  private void scanAll(Map<Long, Supplier<List<? extends MediaStream<?>>>> suppliers, AtomicBoolean cancelled) {
    LOGGER.info("Initiating scan with " + suppliers.size() + " scanners");

    for(Map.Entry<Long, Supplier<List<? extends MediaStream<?>>>> entry : suppliers.entrySet()) {
      try {
        long scannerId = entry.getKey();
        List<? extends MediaStream<?>> results = entry.getValue().get();

        try(AutoReentrantLock lock = mediaManagerLock.lock()) {
          if(cancelled.get()) {
            return;
          }

          LOGGER.fine("Scanner returned " + results.size() + " items: " + entry.getValue());

          Map<String, LocalMedia> existingLocalMedias = mediaStore.findByScannerId(scannerId);

          for(MediaStream<?> scannedStream : results) {
            try {
              LocalMedia existingLocalMedia = existingLocalMedias.remove(scannedStream.getStreamPrint().getIdentifier());

              if(existingLocalMedia == null) {
                LOGGER.finer("New Stream found: " + scannedStream);

                mediaStore.store(toLocalMedia(scannerId, null, scannedStream));

                localMediaManager.add(scannedStream);
              }
              else {
                MediaStream<?> existingMediaStream = toMediaStream(existingLocalMedia);

                if(existingMediaStream == null) {
                  LOGGER.finer("Error decoding existing Stream, replacing with new version: " + existingLocalMedia);

                  mediaStore.store(toLocalMedia(scannerId, null, scannedStream));

                  localMediaManager.add(scannedStream);
                }
                else if(existingLocalMedia.getDeleteTime() != null) {
                  LOGGER.finer("Existing Stream undeleted: " + existingMediaStream);

                  existingLocalMedia.setDeleteTime(null);

                  mediaStore.store(existingLocalMedia);

                  localMediaManager.add(existingMediaStream);
                }
                else if(!scannedStream.getAttributes().equals(existingMediaStream.getAttributes())) {
                  LOGGER.finer("Existing stream modified: " + scannedStream);

                  mediaStore.store(toLocalMedia(scannerId, null, scannedStream));

                  localMediaManager.add(scannedStream);
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

          LocalDateTime now = LocalDateTime.now();

          for(LocalMedia deletedMedia : existingLocalMedias.values()) {
            if(deletedMedia.getDeleteTime() == null) {
              LOGGER.finer("Existing Stream deleted: " + deletedMedia);

              deletedMedia.setDeleteTime(now);

              mediaStore.store(deletedMedia);

              MediaStream<?> mediaStream = toMediaStream(deletedMedia);

              if(mediaStream != null) {
                localMediaManager.remove(mediaStream);
              }
            }
          }
        }
      }
      catch(Throwable t) {
        LOGGER.severe("Exception while running Scanner: " + entry.getValue() + ": " + Throwables.formatAsOneLine(t));
      }
    }
  }

  private static MediaStream<?> toMediaStream(LocalMedia localMedia) {
    try {
      //System.out.println(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(OBJECT_MAPPER.readValue(localMedia.getJson(), Map.class)));
      return OBJECT_MAPPER.readValue(localMedia.getJson(), MediaStream.class);
    }
    catch(JsonProcessingException e) {
      LOGGER.warning("Exception while decoding LocalMedia: " + Throwables.formatAsOneLine(e));

      return null;
    }
    catch(IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static LocalMedia toLocalMedia(long scannerId, LocalDateTime deleteTime, MediaStream<?> mediaStream) {
    try {
      LocalMedia localMedia = new LocalMedia();

      localMedia.setId(mediaStream.getStreamPrint().getIdentifier());
      localMedia.setScannerId(scannerId);
      localMedia.setDeleteTime(deleteTime);
      localMedia.setJson(OBJECT_MAPPER.writeValueAsBytes(mediaStream));

      return localMedia;
    }
    catch(JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
