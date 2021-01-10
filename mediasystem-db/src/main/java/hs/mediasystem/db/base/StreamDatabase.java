package hs.mediasystem.db.base;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamDatabase {
  private static final List<String> EMPTY_LIST = List.of();

  @Inject private Database database;

  public void forEach(Consumer<StreamRecord> consumer) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      Map<Integer, List<String>> identifiersByStreamId = new HashMap<>();

      tx.select(r -> identifiersByStreamId.computeIfAbsent(r.getStreamId(), k -> new ArrayList<>()).add(r.getIdentifier()), StreamIdentifierRecord.class);
      tx.select(r -> {
        r.setIdentifiers(identifiersByStreamId.getOrDefault(r.getId(), EMPTY_LIST));
        consumer.accept(r);
      }, StreamRecord.class);
    }
  }

  public void store(StreamRecord record) {
    try(Transaction tx = database.beginTransaction()) {
      if(record.getId() != null) {
        tx.delete("stream_identifier", "stream_id = ?", record.getId());
      }

      if(tx.selectUnique(StreamRecord.class, "id = ?", record.getId()) == null) {
        tx.insert(record);
      }
      else {
        tx.update(record);
      }

      storeM2M(tx, record);

      tx.commit();
    }
  }

  public Optional<StreamRecord> find(int importSourceId, int contentId, String name) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      return tx.selectOptional(StreamRecord.class, "scanner_id = ? AND content_id = ? AND name = ?", importSourceId, contentId, name);
    }
  }

  public void delete(int id) {
    try(Transaction tx = database.beginTransaction()) {
      tx.delete(StreamRecord.class, id);
      tx.commit();
    }
  }

  public void delete(int importSourceId, int contentId, String name) {
    try(Transaction tx = database.beginTransaction()) {
      tx.selectOptional(StreamRecord.class, "scanner_id = ? AND content_id = ? AND name = ?", importSourceId, contentId, name)
        .ifPresent(r -> tx.delete(StreamRecord.class, r.getId()));
      tx.commit();
    }
  }

  private static void storeM2M(Transaction tx, StreamRecord record) {
    if(record.getIdentifiers() != null) {
      for(String identifier : record.getIdentifiers()) {
        StreamIdentifierRecord m2mRecord = new StreamIdentifierRecord();

        m2mRecord.setIdentifier(identifier);
        m2mRecord.setStreamId(record.getId());

        tx.insert(m2mRecord);
      }
    }
  }
}
