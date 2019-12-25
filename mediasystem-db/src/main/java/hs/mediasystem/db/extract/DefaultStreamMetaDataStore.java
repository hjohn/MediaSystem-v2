package hs.mediasystem.db.extract;

import com.fasterxml.jackson.core.JsonProcessingException;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.mediasystem.db.streamids.StreamIdRecord;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamMetaData;
import hs.mediasystem.mediamanager.StreamMetaDataStore;
import hs.mediasystem.scanner.api.StreamID;

import java.io.IOException;
import java.time.Instant;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefaultStreamMetaDataStore implements StreamMetaDataStore {
  @Inject private Database database;
  @Inject private StreamMetaDataCodec codec;

  public void store(StreamMetaData streamMetaData) {
    try(Transaction tx = database.beginTransaction()) {
      boolean exists = tx.selectUnique(StreamMetaDataRecord.class, "stream_id = ?", streamMetaData.getStreamId().asInt()) != null;

      StreamMetaDataRecord record = toRecord(streamMetaData);

      if(exists) {
        tx.update(record);
      }
      else {
        tx.insert(record);
      }

      tx.commit();
    }
  }

  public Stream<Integer> streamUnindexedStreamIds() {
    return database.stream(StreamIdRecord.class, "lastseentime IS NULL AND id NOT IN (SELECT stream_id FROM stream_metadata)").map(StreamIdRecord::getId);
  }

  public void storeImage(int streamId, int index, byte[] image) {
    try(Transaction tx = database.beginTransaction()) {
      StreamMetaDataSnapshotRecord record = new StreamMetaDataSnapshotRecord();

      record.setStreamId(streamId);
      record.setIndex(index);
      record.setImage(image);

      tx.insert(record);
      tx.commit();
    }
  }

  private StreamMetaDataRecord toRecord(StreamMetaData streamMetaData) {
    try {
      StreamMetaDataRecord record = new StreamMetaDataRecord();

      record.setStreamId(streamMetaData.getStreamId().asInt());
      record.setVersion(1);
      record.setLastModificationTime(Instant.now().toEpochMilli());
      record.setJson(codec.encode(streamMetaData));

      return record;
    }
    catch(JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private StreamMetaData toObject(StreamMetaDataRecord record) {
    try {
      return codec.decode(record.getJson());
    }
    catch(IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public StreamMetaData find(StreamID streamId) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      StreamMetaDataRecord record = tx.selectUnique(StreamMetaDataRecord.class, "stream_id = ?", streamId.asInt());

      return record == null ? null : toObject(record);
    }
  }

  @Override
  public byte[] readSnapshot(StreamID streamId, int snapshotIndex) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      StreamMetaDataSnapshotRecord record = tx.selectUnique(StreamMetaDataSnapshotRecord.class, "stream_id = ? AND index = ?", streamId.asInt(), snapshotIndex);

      return record == null ? null : record.getImage();
    }
  }
}
