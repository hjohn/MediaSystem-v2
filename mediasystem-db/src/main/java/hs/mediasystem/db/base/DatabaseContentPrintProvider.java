package hs.mediasystem.db.base;

import hs.mediasystem.db.contentprints.ContentPrintDatabase;
import hs.mediasystem.db.uris.UriDatabase;
import hs.mediasystem.db.uris.UriRecord;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrint;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrintProvider;
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
public class DatabaseContentPrintProvider implements ContentPrintProvider {
  private static final Logger LOGGER = Logger.getLogger(DatabaseContentPrintProvider.class.getName());

  @Inject private MediaHash mediaHash;
  @Inject private UriDatabase uriDatabase;
  @Inject private ContentPrintDatabase contentPrintDatabase;

  // Note: lock can be held a long time currently (>5 seconds when hashing over network)
  private final AutoReentrantLock lock = new AutoReentrantLock();
  private final Set<ContentID> seenIds = new HashSet<>();  // contains all id's that have are in database and have been seen recently
  private final Set<ContentID> markedIds = new HashSet<>();  // contains all id's that have been marked for deletion
  private final Map<ContentID, ContentPrint> contentPrints = new HashMap<>();

  private Map<String, ContentID> contentIds;

  @PostConstruct
  private void postConstruct() {
    contentIds = uriDatabase.findAll(UriRecord::getUri, r -> new ContentID(r.getContentId()));

    contentPrintDatabase.forEach(r -> {
      ContentID contentId = new ContentID(r.getId());

      contentPrints.put(contentId, new ContentPrint(new ContentID(r.getId()), r.getSize(), r.getLastModificationTime(), r.getHash()));

      if(r.getLastSeenTime() != null) {
        markedIds.add(contentId);
      }
    });

    Thread thread = new Thread(this::backgroundContentIdRemovalThread, "DatabaseContentPrintProvider");

    thread.setPriority(Thread.NORM_PRIORITY - 2);
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Gets or creates a {@link ContentID} for the given resource.  If the content
   * (data, size and mod time) was seen before, this will return the same {@link ContentID}.<p>
   *
   * For resources that were unknown or couldn't be matched, a new {@link ContentID}
   * is returned.<p>
   *
   * Note that it is possible for the same {@link ContentID} to be returned for different
   * uri's -- this simple means that the two uri's point to resources that are copies.
   */
  @Override
  public ContentPrint get(StringURI uri, Long size, long lastModificationTime) throws IOException {
    try(Key key = lock.lock()) {
      ContentID existingContentId = contentIds.get(uri.toString());

      if(existingContentId != null) {
        ContentPrint print = contentPrints.get(existingContentId);

        if(Objects.equals(print.getSize(), size) && print.getLastModificationTime() == lastModificationTime) {
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
           * Unlike files, directories donot have a specific signature that
           * needs to be tracked for, for example, watched state.
           */

          markSeen(existingContentId);

          return updateDirectory(uri, existingContentId, lastModificationTime);
        }
      }

      /*
       * There was either no existing content id for the given uri, or the size
       * and/or last modification time did not match with an existing content id
       * (and it was not a directory in the second case).
       *
       * In either case, a new or existing content id will be found, and linked
       * to the uri.
       *
       * No attempt will be made to find another uri by doing a reverse look-up
       * using the hash, the uri is simply inserted and linked to a new (or
       * existing) content id.
       */

      ContentPrint contentPrint = link(uri, size, lastModificationTime);

      markSeen(contentPrint.getId());

      return contentPrint;
    }
  }

  @Override
  public ContentPrint get(ContentID contentId) {
    try(Key key = lock.lock()) {
      return contentPrints.get(contentId);
    }
  }

  /**
   * Marks the given id as seen.  This is used to track which id's are still active, and
   * which can be removed.
   *
   * @param id an id to mark as seen
   */
  private void markSeen(ContentID id) {
    seenIds.add(id);
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

  private ContentPrint updateDirectory(StringURI uri, ContentID contentId, long lastModificationTime) throws IOException {
    byte[] hash = createHash(uri);
    contentPrintDatabase.update(contentId, null, lastModificationTime, hash);

    ContentPrint contentPrint = new ContentPrint(contentId, null, lastModificationTime, hash);

    // No need to update contentIds or the uriStore as the name and id remains unchanged.
    contentPrints.put(contentId, contentPrint);

    return contentPrint;
  }

  private ContentPrint link(StringURI uri, Long size, long lastModificationTime) throws IOException {
    byte[] hash = createHash(uri);
    int id = contentPrintDatabase.findOrAdd(size, lastModificationTime, hash);
    uriDatabase.store(uri.toString(), id);

    ContentID contentId = new ContentID(id);
    ContentPrint contentPrint = new ContentPrint(contentId, size, lastModificationTime, hash);

    contentIds.put(uri.toString(), contentId);
    contentPrints.put(contentId, contentPrint);

    return contentPrint;
  }

  private byte[] createHash(StringURI uri) throws IOException {
    Path path = Paths.get(uri.toURI());

    if(Files.isRegularFile(path)) {
      return mediaHash.computeFileHash(path);
    }

    return mediaHash.computeDirectoryHash(path);
  }
}
