package hs.mediasystem.db.extract;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.mediasystem.db.uris.UriDatabase;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.work.StreamMetaData;
import hs.mediasystem.util.NamedThreadFactory;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.bg.BackgroundTaskRegistry;
import hs.mediasystem.util.bg.BackgroundTaskRegistry.Workload;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
  private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1, new NamedThreadFactory("MediaMetaDataExtractor", Thread.NORM_PRIORITY - 3, true));

  @Inject private UriDatabase uriDatabase;
  @Inject private DefaultStreamMetaDataStore metaDataStore;
  @Inject private StreamMetaDataFactory factory;
  @Inject private Database database;

  private final Set<Integer> recentFailures = new ConcurrentSkipListSet<>();

  @PostConstruct
  private void postConstruct() {
    EXECUTOR.scheduleWithFixedDelay(this::extract, 300, 30, TimeUnit.SECONDS);
    EXECUTOR.scheduleWithFixedDelay(() -> recentFailures.clear(), 24, 48, TimeUnit.HOURS);
  }

  private void extract() {
    try(Stream<Integer> stream = metaDataStore.streamUnindexedContentIds()) {
      List<Integer> contentIds = stream.filter(cid -> !recentFailures.contains(cid)).collect(Collectors.toList());

      stream.close();  // ends transaction

      if(contentIds.size() > 0) {
        WORKLOAD.start(contentIds.size());

        for(int contentId : contentIds) {
          try {
            createMetaData(contentId);
          }
          catch(Exception e) {
            LOGGER.warning("Error while storing stream metadata in database for content id " + contentId + ": " + Throwables.formatAsOneLine(e));

            recentFailures.add(contentId);
          }
          finally {
            WORKLOAD.complete();
          }
        }
      }
    }
  }

  private void createMetaData(int contentId) throws Exception {
    File file = uriDatabase.findUris(contentId).stream()
      .map(URI::create)
      .map(Paths::get)
      .map(Path::toFile)
      .filter(File::exists)
      .findFirst()
      .orElseThrow(() -> new FileNotFoundException("URI not available or pointed to a missing resource for given content id: " + contentId));

    createMetaData(new ContentID(contentId), file);
  }

  private void createMetaData(ContentID contentId, File file) throws Exception {
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
}
