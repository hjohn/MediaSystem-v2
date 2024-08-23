package hs.mediasystem.db.base;

import hs.mediasystem.db.contentprints.ContentPrintDatabase;
import hs.mediasystem.db.contentprints.ContentPrintRecord;
import hs.mediasystem.db.core.domain.ContentPrint;
import hs.mediasystem.db.uris.UriDatabase;
import hs.mediasystem.db.uris.UriRecord;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.util.AutoReentrantLock;
import hs.mediasystem.util.AutoReentrantLock.Key;
import hs.mediasystem.util.MediaHash;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides {@link ContentPrint}s.<p>
 *
 * A {@link ContentPrint} is a unique signature of a file or directory.  For files,
 * this signature includes its size, modification time and hash.  If a file is renamed
 * but these values match, it is considered to be the same stream, and the same ContentPrint
 * will be returned.<p>
 *
 * Any other change to a file will result in it being assigned a new {@link ContentID}.<p>
 *
 * For a directory, the same signatures are gathered, apart from the size which is <code>null</code>
 * for directories.  When a directory is renamed, but a matching modification time and hash
 * can be found, its {@link ContentID} remains unchanged.  Similarly, when a directory is
 * modified (with new files for example) but its name remains unchanged, its ContentPrint is
 * updated but the {@link ContentID} is kept the same.<p>
 */
@Singleton
public class DatabaseContentPrintProvider {
  private static final Logger LOGGER = Logger.getLogger(DatabaseContentPrintProvider.class.getName());

  @Inject private MediaHash mediaHash;
  @Inject private UriDatabase uriDatabase;
  @Inject private ContentPrintDatabase contentPrintDatabase;

  private final AutoReentrantLock lock = new AutoReentrantLock();
  private final Set<ContentID> seenIds = ConcurrentHashMap.newKeySet();  // contains all id's that have are in database and have been seen recently
  private final Set<ContentID> markedIds = new HashSet<>();  // contains all id's that have been marked for deletion
  private final Map<ContentID, ContentPrint> contentPrints = new HashMap<>();

  private Map<String, ContentID> contentIds;

  @PostConstruct
  private void postConstruct() {
    contentIds = uriDatabase.findAll(UriRecord::uri, r -> new ContentID(r.contentId()));

    contentPrintDatabase.forEach(r -> {
      ContentPrint contentPrint = fromRecord(r);

      contentPrints.put(contentPrint.getId(), contentPrint);

      if(r.lastSeenTime() != null) {
        markedIds.add(contentPrint.getId());
      }
    });

    Thread thread = new Thread(this::backgroundContentIdRemovalThread, "DatabaseContentPrintProvider");

    thread.setPriority(Thread.NORM_PRIORITY - 2);
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Gets or creates a {@link ContentID} for the given resource.  If the content
   * (data, size and last modification time) was seen before, this will return the same {@link ContentID}.<p>
   *
   * For resources that were unknown or couldn't be matched, a new {@link ContentID}
   * is returned.<p>
   *
   * Note that it is possible for the same {@link ContentID} to be returned for different
   * uri's -- this simple means that the two uri's point to resources that are copies.
   *
   * @param location a location, cannot be {@code null}
   * @param size a size, can be {@code null} but can't be negative
   * @param lastModificationTime an {@link Instant}, cannot be  {@code null}
   * @return a {@link ContentPrint}, never {@code null}
   * @throws IOException when an IO error occurs
   */
  public ContentPrint get(URI location, Long size, Instant lastModificationTime) throws IOException {
    try(Key key = lock.lock()) {
      ContentID existingContentId = contentIds.get(location.toString());
      ContentPrint print = contentPrints.get(existingContentId);
      Instant modTime = Instant.ofEpochMilli(lastModificationTime.toEpochMilli());  // reduce accuracy for comparison (stored as milliseconds in database)

      key.earlyUnlock();

      if(existingContentId != null) {
        if(Objects.equals(print.getSize(), size) && print.getLastModificationTime().equals(modTime)) {
          markSeen(existingContentId);

          return print;
        }

        if(size == null) {  // is it a directory?

          /*
           * For directories, if the name matched, the ContentPrint is always
           * simply updated, and the same ContentID is kept.  This is because
           * adding or removing files from a directory will change its last
           * modification time and hash, but does not affect how it would be
           * matched.
           *
           * Unlike files, directories do not have a specific signature that
           * needs to be tracked for, for example, watched state.
           */

          markSeen(existingContentId);

          return updateDirectory(location, existingContentId, modTime);
        }
      }

      /*
       * There was either no existing content id for the given location, or the size
       * and/or last modification time did not match with an existing content id
       * (and it was not a directory in the second case).
       *
       * In either case, a new or existing content id will be found, and linked
       * to the location.
       *
       * No attempt will be made to find another location by doing a reverse look-up
       * using the hash, the location is simply inserted and linked to a new (or
       * existing) content id.
       */

      ContentPrint contentPrint = link(location, size, modTime);

      markSeen(contentPrint.getId());

      return contentPrint;
    }
  }

  /**
   * Marks the given id as seen.  This is used to track which id's are still active, and
   * which can be removed.
   *
   * @param id an id to mark as seen
   */
  private void markSeen(ContentID id) {
    try(Key key = lock.lock()) {
      seenIds.add(id);
    }
  }

  /**
   * Thread for background removal of ContentID's that have not been seen in a while.<p>
   *
   * Note that removal of a ContentID also removes all associated data, including meta
   * data, viewed status, etc. through cascaded deletes.  Removal therefore should be
   * done conservatively.<p>
   *
   * Policy for now is to first mark items for removal, but only remove them a few
   * months later.
   */
  private void backgroundContentIdRemovalThread() {
    for(;;) {
      try {
        Thread.sleep(Duration.ofMinutes(60).toMillis());

        try(Key key = lock.lock()) {
          // Create set of ids to mark in database with current time, excluding those already marked or recently seen:
          Set<ContentID> idsToMark = contentPrints.keySet().stream()
            .filter(id -> !markedIds.contains(id))
            .filter(id -> !seenIds.contains(id))
            .collect(Collectors.toSet());

          // Create set of ids that are currently marked but were seen:
          Set<ContentID> idsToUnmark = markedIds.stream()
            .filter(seenIds::contains)
            .collect(Collectors.toSet());

          seenIds.clear();

          // Safe to unlock, code beyond this should only be accessing markedIds, which is not used anywhere else:
          key.earlyUnlock();

          if(!idsToMark.isEmpty()) {
            LOGGER.fine("Marking " + idsToMark.size() + " items as NOT seen recently: " + idsToMark);

            contentPrintDatabase.markSeen(idsToMark);
            markedIds.addAll(idsToMark);
          }

          if(!idsToUnmark.isEmpty()) {
            LOGGER.fine("Unmarking " + idsToUnmark.size() + " items: " + idsToUnmark);

            contentPrintDatabase.unmarkSeen(idsToUnmark);
            markedIds.removeAll(idsToUnmark);
          }

          // TODO contentPrintStore.pruneNotSeenSince("3 months");
          // ...delete from ContentPrints etc.
          // Might be better to only prune on startup
        }
      }
      catch(InterruptedException e) {
        // Ignore;
      }
    }
  }

  private ContentPrint updateDirectory(URI location, ContentID contentId, Instant lastModificationTime) throws IOException {
    byte[] hash = createHash(location);

    try(Key key = lock.lock()) {
      ContentPrintRecord record = contentPrintDatabase.update(contentId, null, lastModificationTime, hash);

      ContentPrint contentPrint = fromRecord(record);

      // No need to update contentIds or the uriStore as the name and id remains unchanged.
      contentPrints.put(contentId, contentPrint);

      return contentPrint;
    }
  }

  private ContentPrint link(URI location, Long size, Instant lastModificationTime) throws IOException {
    byte[] hash = createHash(location);

    try(Key key = lock.lock()) {
      ContentPrintRecord contentPrintRecord = contentPrintDatabase.findOrAdd(size, lastModificationTime, hash);

      uriDatabase.store(location.toString(), contentPrintRecord.id());

      ContentPrint contentPrint = fromRecord(contentPrintRecord);

      contentIds.put(location.toString(), contentPrint.getId());
      contentPrints.put(contentPrint.getId(), contentPrint);

      return contentPrint;
    }
  }

  private static ContentPrint fromRecord(ContentPrintRecord contentPrintRecord) {
    return new ContentPrint(
      new ContentID(contentPrintRecord.id()),
      contentPrintRecord.size(),
      Instant.ofEpochMilli(contentPrintRecord.lastModificationTime()),
      contentPrintRecord.hash(),
      Instant.ofEpochMilli(contentPrintRecord.creationMillis())
    );
  }

  private byte[] createHash(URI location) throws IOException {
    Path path = Paths.get(location);

    if(Files.isRegularFile(path)) {
      return mediaHash.computeFileHash(path);
    }

    return mediaHash.computeDirectoryHash(path);
  }
}
