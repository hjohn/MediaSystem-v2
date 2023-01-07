package hs.mediasystem.db.extract;

import hs.database.core.Database;
import hs.ddif.annotations.Produces;
import hs.mediasystem.db.events.EventSerializer;
import hs.mediasystem.db.events.PersistentEventStore;
import hs.mediasystem.db.events.Serializer;
import hs.mediasystem.db.events.SerializerException;
import hs.mediasystem.db.jackson.SealedTypeSerializer;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.work.StreamMetaData;
import hs.mediasystem.mediamanager.StreamMetaDataStore;
import hs.mediasystem.util.events.PersistentEventStream;
import hs.mediasystem.util.events.cache.CachingEventStore;
import hs.mediasystem.util.events.streams.Source;
import hs.mediasystem.util.exception.Throwables;

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
  private final PersistentEventStream<StreamMetaDataEvent> persistentEventStream;
  private final Set<ContentID> metaDataAvailable = new HashSet<>();

  @Inject
  public DefaultStreamMetaDataStore(Database database) {
    Serializer<StreamMetaDataEvent> serializer = new SealedTypeSerializer<>(StreamMetaDataEvent.class);

    EventSerializer<StreamMetaDataEvent> eventSerializer = new EventSerializer<>() {
      @Override
      public byte[] serialize(StreamMetaDataEvent value) throws SerializerException {
        return serializer.serialize(value);
      }

      @Override
      public StreamMetaDataEvent unserialize(byte[] serialized) throws SerializerException {
        return serializer.unserialize(serialized);
      }

      @Override
      public Type extractType(StreamMetaDataEvent event) {
        return switch(event.getClass().getSimpleName()) {
          case "Updated" -> Type.FULL;
          case "Removed" -> Type.DELETE;
          default -> throw new IllegalStateException("Unknown case: " + event.getClass().getSimpleName());
        };
      }

      @Override
      public String extractAggregateId(StreamMetaDataEvent event) {
        return "" + event.id().asInt();
      }
    };

    this.eventStore = new PersistentEventStore<>(database, StreamMetaDataEvent.class, eventSerializer);
    this.persistentEventStream = new PersistentEventStream<>(new CachingEventStore<>(eventStore));
  }

  @Produces
  Source<StreamMetaDataEvent> events() {
    return persistentEventStream.plain();
  }

  @PostConstruct
  private void postConstruct() {
    if(!eventStore.hasEvents()) {
      // Create events based on old database table:
      database.forEach(r -> {
        try {
          StreamMetaData smd = codec.decode(r.getJson());

          persistentEventStream.push(new StreamMetaDataEvent.Updated(smd));
        }
        catch(IOException e) {
          LOGGER.warning("Exception decoding StreamMetaDataRecord: " + r + ": " + Throwables.formatAsOneLine(e));
        }
      });
    }

    this.persistentEventStream.plain().subscribe(getClass().getSimpleName(), e -> {
      if(e instanceof StreamMetaDataEvent.Updated u) {
        metaDataAvailable.add(u.streamMetaData().contentId());
      }
      else if(e instanceof StreamMetaDataEvent.Removed r) {
        metaDataAvailable.remove(r.id());
      }
    });
  }

  void store(StreamMetaData streamMetaData) {
    persistentEventStream.push(new StreamMetaDataEvent.Updated(streamMetaData));
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
