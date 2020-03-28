package hs.mediasystem.db.extract;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.mediasystem.db.uris.UriDatabase;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.work.StreamMetaData;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.bg.BackgroundTaskRegistry;
import hs.mediasystem.util.bg.BackgroundTaskRegistry.Workload;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaMetaDataExtractor {
  private static final Logger LOGGER = Logger.getLogger(MediaMetaDataExtractor.class.getName());
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Extracting Metadata");
  private static final long HOUR = 60 * 60 * 1000;

  @Inject private UriDatabase uriDatabase;
  @Inject private DefaultStreamMetaDataStore metaDataStore;
  @Inject private StreamMetaDataFactory factory;
  @Inject private Database database;

  @PostConstruct
  private void postConstruct() {
    Thread thread = new Thread(this::extractThread);

    thread.setDaemon(true);
    thread.setPriority(Thread.NORM_PRIORITY - 3);
    thread.setName("MediaMetaDataExtractor");
    thread.start();
  }

  private void extractThread() {
    for(;;) {
      try {
        Thread.sleep(HOUR);

        try(Stream<Integer> stream = metaDataStore.streamUnindexedContentIds()) {
          List<Integer> contentIds = stream.collect(Collectors.toList());

          stream.close();  // ends transaction

          WORKLOAD.start(contentIds.size());

          for(int contentId : contentIds) {
            try {
              createMetaData(contentId);
            }
            finally {
              WORKLOAD.complete();
            }
          }
        }

        Thread.sleep(6 * HOUR);
      }
      catch(Exception e) {
        LOGGER.warning("Exception while extracting local metadata: " + Throwables.formatAsOneLine(e));
      }
    }
  }

  private void createMetaData(int contentId) {
    try {
      uriDatabase.findUris(contentId).stream()
        .map(URI::create)
        .map(Paths::get)
        .map(Path::toFile)
        .filter(File::exists)
        .findFirst()
        .ifPresent(file -> createMetaData(new ContentID(contentId), file));
    }
    catch(Exception e) {
      LOGGER.warning("Error while storing stream metadata in database for content id " + contentId + ": " + Throwables.formatAsOneLine(e));
    }
  }

  private void createMetaData(ContentID contentId, File file) {
    try {
      LOGGER.fine("Extracting metadata from: " + file);

      if(file.isDirectory()) {
        StreamMetaData metaData = new StreamMetaData(contentId, Duration.ZERO, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        metaDataStore.store(metaData);
      }
      else {
        try(Transaction tx = database.beginTransaction()) {
          StreamMetaData metaData = factory.generatePreviewImage(contentId, file);

          metaDataStore.store(metaData);

          tx.commit();
        }
      }
    }
    catch(Exception e) {
      LOGGER.warning("Error while decoding stream '" + file + "', contentId " + contentId.asInt() + ": " + Throwables.formatAsOneLine(e));
    }
  }
}
