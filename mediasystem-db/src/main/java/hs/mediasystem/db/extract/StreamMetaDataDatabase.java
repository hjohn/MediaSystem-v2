package hs.mediasystem.db.extract;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.mediasystem.db.streamids.StreamIdRecord;

import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamMetaDataDatabase {
  @Inject private Database database;

  public void forEach(Consumer<StreamMetaDataRecord> consumer) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      tx.select(consumer, StreamMetaDataRecord.class);
    }
  }

  public void store(StreamMetaDataRecord record) {
    try(Transaction tx = database.beginTransaction()) {
      if(tx.selectUnique(StreamMetaDataRecord.class, "stream_id = ?", record.getStreamId()) == null) {
        tx.insert(record);
      }
      else {
        tx.update(record);
      }

      tx.commit();
    }
  }

  public void delete(int id) {
    try(Transaction tx = database.beginTransaction()) {
      tx.delete(StreamMetaDataRecord.class, id);
      tx.commit();
    }
  }

  public Stream<Integer> streamUnindexedStreamIds() {
    return database.stream(StreamIdRecord.class, "lastseentime IS NULL AND id NOT IN (SELECT stream_id FROM stream_metadata)").map(StreamIdRecord::getId);
  }

  public byte[] readSnapshot(int id, int snapshotIndex) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      StreamMetaDataSnapshotRecord record = tx.selectUnique(StreamMetaDataSnapshotRecord.class, "stream_id = ? AND index = ?", id, snapshotIndex);

      return record == null ? null : record.getImage();
    }
  }

  public boolean existsSnapshot(int id, int snapshotIndex) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      return tx.selectUnique("stream_id", "stream_metadata_snapshots", "stream_id = ? AND index = ?", id, snapshotIndex) != null;
    }
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
}
