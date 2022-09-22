package hs.mediasystem.db.extract;

import com.fasterxml.jackson.core.JsonProcessingException;

import hs.ddif.annotations.Produces;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.work.StreamMetaData;
import hs.mediasystem.mediamanager.StreamMetaDataStore;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.events.Event;
import hs.mediasystem.util.events.EventSource;
import hs.mediasystem.util.events.EventStream;
import hs.mediasystem.util.events.InMemoryEventStream;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefaultStreamMetaDataStore implements StreamMetaDataStore {
  private static final Logger LOGGER = Logger.getLogger(DefaultStreamMetaDataStore.class.getName());

  @Inject private StreamMetaDataDatabase database;
  @Inject private StreamMetaDataCodec codec;

  private final EventStream<StreamMetaDataEvent> eventStream = new InMemoryEventStream<>();

  private final Map<ContentID, StreamMetaData> cache = new ConcurrentHashMap<>();

  @Produces
  EventSource<StreamMetaDataEvent> events() {
    return eventStream;
  }

  @PostConstruct
  private void postConstruct() {
    List<Integer> badIds = new ArrayList<>();

    database.forEach(r -> {
      try {
        StreamMetaData smd = codec.decode(r.getJson());

        eventStream.push(new Event<>(new StreamMetaDataEvent.Updated(smd)));

        cache.put(smd.getContentId(), smd);
      }
      catch(IOException e) {
        LOGGER.warning("Exception decoding StreamMetaDataRecord: " + r + ": " + Throwables.formatAsOneLine(e));

        badIds.add(r.getContentId());
      }
    });

    badIds.stream().forEach(database::delete);

    LOGGER.fine("Loaded " + cache.size() + " StreamMetaDataRecords, deleted " + badIds.size() + " bad ones");
  }

  void store(StreamMetaData streamMetaData) {
    database.store(toRecord(streamMetaData));

    eventStream.push(new Event<>(new StreamMetaDataEvent.Updated(streamMetaData)));

    cache.put(streamMetaData.getContentId(), streamMetaData);
  }

  Stream<Integer> streamUnindexedContentIds() {
    return database.streamUnindexedContentIds();
  }

  void storeImage(ContentID contentId, int index, byte[] image) {
    database.storeImage(contentId.asInt(), index, image);
  }

  private StreamMetaDataRecord toRecord(StreamMetaData streamMetaData) {
    try {
      StreamMetaDataRecord record = new StreamMetaDataRecord();

      record.setContentId(streamMetaData.getContentId().asInt());
      record.setVersion(1);
      record.setLastModificationTime(Instant.now().toEpochMilli());
      record.setJson(codec.encode(streamMetaData));

      return record;
    }
    catch(JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Optional<StreamMetaData> find(ContentID contentId) {
    return Optional.ofNullable(cache.get(contentId));
  }

  @Override
  public byte[] readSnapshot(ContentID contentId, int snapshotIndex) {
    return database.readSnapshot(contentId.asInt(), snapshotIndex);
  }

  boolean existsSnapshot(ContentID contentId, int snapshotIndex) {
    return database.existsSnapshot(contentId.asInt(), snapshotIndex);
  }
}
