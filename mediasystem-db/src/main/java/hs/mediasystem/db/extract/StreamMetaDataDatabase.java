package hs.mediasystem.db.extract;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;

import java.util.function.Consumer;

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
      if(tx.selectUnique(StreamMetaDataRecord.class, "content_id = ?", record.getContentId()) == null) {
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

  public byte[] readSnapshot(int id, int snapshotIndex) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      StreamMetaDataSnapshotRecord record = tx.selectUnique(StreamMetaDataSnapshotRecord.class, "content_id = ? AND index = ?", id, snapshotIndex);

      return record == null ? null : record.getImage();
    }
  }

  public boolean existsSnapshot(int id, int snapshotIndex) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      return tx.selectUnique("content_id", "stream_metadata_snapshots", "content_id = ? AND index = ?", id, snapshotIndex) != null;
    }
  }

  public void storeImage(int contentId, int index, byte[] image) {
    try(Transaction tx = database.beginTransaction()) {
      StreamMetaDataSnapshotRecord record = new StreamMetaDataSnapshotRecord();

      record.setContentId(contentId);
      record.setIndex(index);
      record.setImage(image);

      tx.insert(record);
      tx.commit();
    }
  }
}
