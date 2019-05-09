package hs.mediasystem.db.streamids;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.mediasystem.scanner.api.StreamID;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DatabaseStreamIdStore {
  @Inject private Database database;

  public <T, U> Map<T, U> findAll(Function<StreamIdRecord, T> keyMapper, Function<StreamIdRecord, U> valueMapper) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      Map<T, U> map = new HashMap<>();

      tx.select(r -> map.put(keyMapper.apply(r), valueMapper.apply(r)), StreamIdRecord.class);

      return map;
    }
  }

  public void forEach(Consumer<StreamIdRecord> consumer) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      tx.select(consumer, StreamIdRecord.class);
    }
  }

  public int findOrAdd(Long size, long lastModificationTime, byte[] hash) {
    try(Transaction tx = database.beginTransaction()) {
      StreamIdRecord record = findStreamDataByHash(hash, size, lastModificationTime);

      if(record != null) {
        return record.getId();
      }

      record = new StreamIdRecord();

      record.setSize(size);
      record.setLastModificationTime(lastModificationTime);
      record.setHash(hash);

      tx.insert(record);
      tx.commit();

      return record.getId();
    }
  }

  private StreamIdRecord findStreamDataByHash(byte[] hash, Long size, long lastModificationTime) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      if(size == null) {
        return tx.selectUnique(StreamIdRecord.class, "hash = ? AND size IS NULL AND modtime = ?", hash, lastModificationTime);
      }

      return tx.selectUnique(StreamIdRecord.class, "hash = ? AND size = ? AND modtime = ?", hash, size, lastModificationTime);
    }
  }

  public void markSeen(Set<StreamID> idsToMark) {
    try(Transaction tx = database.beginTransaction()) {
      long now = Instant.now().toEpochMilli();

      for(StreamID streamID : idsToMark) {
        tx.update("stream_ids", streamID.asInt(), Map.of("lastseentime", now));
      }

      tx.commit();
    }
  }

  public void unmarkSeen(Set<StreamID> idsToUnmark) {
    try(Transaction tx = database.beginTransaction()) {
      for(StreamID streamID : idsToUnmark) {
        tx.update("stream_ids", streamID.asInt(), Map.of("lastseentime", null));
      }

      tx.commit();
    }
  }
}
