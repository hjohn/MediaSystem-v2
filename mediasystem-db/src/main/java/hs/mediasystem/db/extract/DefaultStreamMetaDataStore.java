package hs.mediasystem.db.extract;

import hs.database.core.Database;
import hs.ddif.annotations.Produces;
import hs.mediasystem.db.events.PersistentEventStore;
import hs.mediasystem.db.jackson.SealedTypeSerializer;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.work.StreamMetaData;
import hs.mediasystem.mediamanager.StreamMetaDataStore;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.events.EventSource;
import hs.mediasystem.util.events.EventStream;
import hs.mediasystem.util.events.SimpleEventStream;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefaultStreamMetaDataStore implements StreamMetaDataStore {
  private static final Logger LOGGER = Logger.getLogger(DefaultStreamMetaDataStore.class.getName());

  @Inject private StreamMetaDataDatabase database;
  @Inject private StreamMetaDataCodec codec;

  private final PersistentEventStore<StreamMetaDataEvent> eventStore;
  private final EventStream<StreamMetaDataEvent> eventStream;
  private final Set<ContentID> metaDataAvailable = new HashSet<>();

  @Inject
  public DefaultStreamMetaDataStore(Database database) {
    this.eventStore = new PersistentEventStore<>(database, "StreamMetaDataEvent", new SealedTypeSerializer<>(StreamMetaDataEvent.class));
    this.eventStream = new SimpleEventStream<>(eventStore);
  }

  @Produces
  EventSource<StreamMetaDataEvent> events() {
    return eventStream;
  }

  @PostConstruct
  private void postConstruct() {
    if(!eventStore.hasEvents()) {
      // Create events based on old database table:
      database.forEach(r -> {
        try {
          StreamMetaData smd = codec.decode(r.getJson());

          eventStream.push(new StreamMetaDataEvent.Updated(smd));
        }
        catch(IOException e) {
          LOGGER.warning("Exception decoding StreamMetaDataRecord: " + r + ": " + Throwables.formatAsOneLine(e));
        }
      });
    }

    this.eventStream.subscribeAndWait(e -> {
      if(e instanceof StreamMetaDataEvent.Updated u) {
        metaDataAvailable.add(u.streamMetaData().contentId());
      }
      else if(e instanceof StreamMetaDataEvent.Removed r) {
        metaDataAvailable.remove(r.id());
      }
    });
  }

  void store(StreamMetaData streamMetaData) {
    eventStream.push(new StreamMetaDataEvent.Updated(streamMetaData));
  }

  boolean exists(ContentID contentId) {
    return metaDataAvailable.contains(contentId);
  }

  void storeImage(ContentID contentId, int index, byte[] image) {
    database.storeImage(contentId.asInt(), index, image);
  }

  @Override
  public byte[] readSnapshot(ContentID contentId, int snapshotIndex) {
    return database.readSnapshot(contentId.asInt(), snapshotIndex);
  }

  boolean existsSnapshot(ContentID contentId, int snapshotIndex) {
    return database.existsSnapshot(contentId.asInt(), snapshotIndex);
  }
}
