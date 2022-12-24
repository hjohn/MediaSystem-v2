package hs.mediasystem.db.extract;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.mediasystem.db.base.DatabaseContentPrintProvider;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.bytedeco.javacpp.avutil;

@Singleton
public class MediaMetaDataExtractor {
  private static final Logger LOGGER = Logger.getLogger(MediaMetaDataExtractor.class.getName());
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Extracting Metadata");
  private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1, new NamedThreadFactory("MediaMetaDataExtractor", Thread.NORM_PRIORITY - 3, true));

  @Inject private UriDatabase uriDatabase;
  @Inject private DefaultStreamMetaDataStore metaDataStore;
  @Inject private DatabaseContentPrintProvider contentPrintProvider;
  @Inject private StreamMetaDataFactory factory;
  @Inject private Database database;

  private final Set<ContentID> recentFailures = new ConcurrentSkipListSet<>(Comparator.comparingInt(ContentID::asInt));

  @PostConstruct
  private void postConstruct() {
    EXECUTOR.scheduleWithFixedDelay(this::extract, 300, 30, TimeUnit.SECONDS);
    EXECUTOR.scheduleWithFixedDelay(() -> recentFailures.clear(), 24, 48, TimeUnit.HOURS);

    avutil.av_log_set_level(avutil.AV_LOG_FATAL);
  }

  private void extract() {
    try {
      List<ContentID> contentIds = contentPrintProvider.recentlySeen()
        .filter(Predicate.not(metaDataStore::exists))
        .filter(Predicate.not(recentFailures::contains))
        .toList();

      if(contentIds.size() > 0) {
        WORKLOAD.start(contentIds.size());

        for(ContentID contentId : contentIds) {
          try {
            createMetaData(contentId);
          }
          catch(Throwable e) {
            LOGGER.warning("Error while storing stream metadata in database for content id " + contentId + ": " + Throwables.formatAsOneLine(e));

            recentFailures.add(contentId);
          }
          finally {
            WORKLOAD.complete();
          }
        }
      }
    }
    catch(Throwable e) {
      LOGGER.log(Level.SEVERE, "Exception in scheduled task", e);
    }
  }

  private void createMetaData(ContentID contentId) throws Exception {
    File file = uriDatabase.findUris(contentId.asInt()).stream()
      .map(URI::create)
      .map(Paths::get)
      .map(Path::toFile)
      .filter(File::exists)
      .findFirst()
      .orElseThrow(() -> new FileNotFoundException("URI not available or pointed to a missing resource for given content id: " + contentId));

    createMetaData(contentId, file);
  }

  private void createMetaData(ContentID contentId, File file) throws Exception {
    LOGGER.fine("Extracting metadata from: " + file);

    if(file.isDirectory()) {
      StreamMetaData metaData = new StreamMetaData(contentId, null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

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
