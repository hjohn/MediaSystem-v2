package hs.mediasystem.db;

import hs.mediasystem.db.streamids.DatabaseStreamIdStore;
import hs.mediasystem.db.uris.DatabaseUriStore;
import hs.mediasystem.db.uris.UriRecord;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.scanner.api.StreamPrint;
import hs.mediasystem.scanner.api.StreamPrintProvider;
import hs.mediasystem.util.AutoReentrantLock;
import hs.mediasystem.util.AutoReentrantLock.Key;
import hs.mediasystem.util.MediaHash;
import hs.mediasystem.util.StringURI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DatabaseStreamPrintProvider implements StreamPrintProvider {
  private static final Logger LOGGER = Logger.getLogger(DatabaseStreamPrintProvider.class.getName());

  @Inject private MediaHash mediaHash;
  @Inject private DatabaseUriStore uriStore;
  @Inject private DatabaseStreamIdStore streamIdStore;

  // Note: lock can be held a long time currently (>5 seconds when hashing over network)
  private final AutoReentrantLock lock = new AutoReentrantLock();
  private final Set<StreamID> seenIds = new HashSet<>();  // contains all id's that have are in database and have been seen recently
  private final Set<StreamID> markedIds = new HashSet<>();  // contains all id's that have been marked for deletion
  private final Map<StreamID, StreamPrint> streamPrints = new HashMap<>();

  private Map<String, StreamID> streamIds;

  @PostConstruct
  private void postConstruct() {
    streamIds = uriStore.findAll(UriRecord::getUri, r -> new StreamID(r.getStreamId()));

    streamIdStore.forEach(r -> {
      StreamID streamId = new StreamID(r.getId());

      streamPrints.put(streamId, new StreamPrint(new StreamID(r.getId()), r.getSize(), r.getLastModificationTime(), r.getHash()));

      if(r.getLastSeenTime() != null) {
        markedIds.add(streamId);
      }
    });

    Thread thread = new Thread(this::backgroundStreamIdRemovalThread, "DatabaseStreamPrintProvider");

    thread.setPriority(Thread.NORM_PRIORITY - 2);
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Gets or creates a {@link StreamID} for the given resource.  If the content
   * (data, size and mod time) was seen before, this will return the same {@link StreamID}.<p>
   *
   * For resources that were unknown or couldn't be matched, a new {@link StreamID}
   * is returned.<p>
   *
   * Note that it is possible for the same {@link StreamID} to be returned for different
   * uri's -- this simple means that the two uri's point to resources that are copies.
   */
  @Override
  public StreamPrint get(StringURI uri, Long size, long lastModificationTime) throws IOException {
    try(Key key = lock.lock()) {
      StreamID existingStreamId = streamIds.get(uri.toString());

      if(existingStreamId != null) {
        StreamPrint print = streamPrints.get(existingStreamId);

        if(Objects.equals(print.getSize(), size) && print.getLastModificationTime() == lastModificationTime) {
          markSeen(print.getId());

          return print;
        }
      }

      /*
       * There was either no existing stream id for the given uri, or the size
       * and/or last modification time did not match with an existing stream id.
       *
       * In either case, a new or existing stream id will be found, and linked
       * to the uri.
       *
       * No attempt will be made to find another uri by doing a reverse look-up
       * using the hash, the uri is simply inserted and linked to a new (or
       * existing) stream id.
       */

      StreamPrint streamPrint = link(uri, size, lastModificationTime);

      markSeen(streamPrint.getId());

      return streamPrint;
    }
  }

  @Override
  public StreamPrint get(StreamID streamId) {
    try(Key key = lock.lock()) {
      return streamPrints.get(streamId);
    }
  }

  /**
   * Marks the given id as seen.  This is used to track which id's are still active, and
   * which can be removed.
   *
   * @param id an id to mark as seen
   */
  private void markSeen(StreamID id) {
    seenIds.add(id);
  }

  /**
   * Thread for background removal of StreamID's that have not been seen in a while.<p>
   *
   * Note that removal of a StreamID also removes all associated data, including meta
   * data, viewed status, etc. through cascaded deletes.  Removal therefore should be
   * done conservatively.<p>
   *
   * Policy for now is to first mark items for removal, but only remove them a few
   * months later.
   */
  private void backgroundStreamIdRemovalThread() {
    for(;;) {
      try {
        Thread.sleep(Duration.ofMinutes(60).toMillis());

        try(Key key = lock.lock()) {
          // Create set of ids to mark in database with current time, excluding those already marked or recently seen:
          Set<StreamID> idsToMark = streamPrints.keySet().stream()
            .filter(id -> !markedIds.contains(id))
            .filter(id -> !seenIds.contains(id))
            .collect(Collectors.toSet());

          // Create set of ids that are currently marked but were seen:
          Set<StreamID> idsToUnmark = markedIds.stream()
            .filter(seenIds::contains)
            .collect(Collectors.toSet());

          seenIds.clear();

          // Safe to unlock, code beyond this should only be accessing markedIds, which is not used anywhere else:
          key.earlyUnlock();

          if(!idsToMark.isEmpty()) {
            LOGGER.fine("Marking " + idsToMark.size() + " items as NOT seen recently: " + idsToMark);

            streamIdStore.markSeen(idsToMark);
            markedIds.addAll(idsToMark);
          }

          if(!idsToUnmark.isEmpty()) {
            LOGGER.fine("Unmarking " + idsToUnmark.size() + " items: " + idsToUnmark);

            streamIdStore.unmarkSeen(idsToUnmark);
            markedIds.removeAll(idsToUnmark);
          }

          // streamIdStore.pruneNotSeenSince("3 months");
          // ...delete from StreamPrints etc.
          // Might be better to only prune on startup
        }
      }
      catch(InterruptedException e) {
        // Ignore;
      }
    }
  }

  private StreamPrint link(StringURI uri, Long size, long lastModificationTime) throws IOException {
    byte[] hash = createHash(uri);
    int id = streamIdStore.findOrAdd(size, lastModificationTime, hash);
    uriStore.store(uri.toString(), id);

    StreamID streamId = new StreamID(id);
    StreamPrint streamPrint = new StreamPrint(streamId, size, lastModificationTime, hash);

    streamIds.put(uri.toString(), streamId);
    streamPrints.put(streamId, streamPrint);

    return streamPrint;
  }

  private byte[] createHash(StringURI uri) throws IOException {
    Path path = Paths.get(uri.toURI());

    if(Files.isRegularFile(path)) {
      return mediaHash.computeFileHash(path);
    }

    return mediaHash.computeDirectoryHash(path);
  }
}
