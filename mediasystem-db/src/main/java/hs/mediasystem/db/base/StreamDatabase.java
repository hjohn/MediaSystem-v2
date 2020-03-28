package hs.mediasystem.db.base;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;

import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamDatabase {
  @Inject private Database database;

  public void forEach(Consumer<StreamRecord> consumer) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      tx.select(r -> {
        r.setIdentifiers(tx.select(StreamIdentifierRecord.class, "content_id = ?", r.getContentId()).stream().map(StreamIdentifierRecord::getIdentifier).collect(Collectors.toList()));
        consumer.accept(r);
      }, StreamRecord.class);
    }
  }

  public void store(StreamRecord record) {
    try(Transaction tx = database.beginTransaction()) {
      tx.delete("stream_identifier", "content_id = ?", record.getContentId());

      if(tx.selectUnique(StreamRecord.class, "content_id = ?", record.getContentId()) == null) {
        tx.insert(record);
      }
      else {
        tx.update(record);
      }

      storeM2M(tx, record);

      tx.commit();
    }
  }

  public void delete(int contentId) {
    try(Transaction tx = database.beginTransaction()) {
      tx.delete(StreamRecord.class, contentId);
      tx.commit();
    }
  }

  private static void storeM2M(Transaction tx, StreamRecord record) {
    if(record.getIdentifiers() != null) {
      for(String identifier : record.getIdentifiers()) {
        StreamIdentifierRecord m2mRecord = new StreamIdentifierRecord();

        m2mRecord.setIdentifier(identifier);
        m2mRecord.setContentId(record.getContentId());

        tx.insert(m2mRecord);
      }
    }
  }
}
