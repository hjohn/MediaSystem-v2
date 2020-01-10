package hs.mediasystem.db.base;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;

import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamDatabase {
  @Inject private Database database;

  public void forEach(Consumer<StreamRecord> consumer) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      tx.select(consumer, StreamRecord.class);
    }
  }

  public void store(StreamRecord record) {
    try(Transaction tx = database.beginTransaction()) {
      if(tx.selectUnique(StreamRecord.class, "stream_id = ?", record.getStreamId()) == null) {
        tx.insert(record);
      }
      else {
        tx.update(record);
      }

      tx.commit();
    }
  }

  public void delete(int streamId) {
    try(Transaction tx = database.beginTransaction()) {
      tx.delete(StreamRecord.class, streamId);
      tx.commit();
    }
  }
}
