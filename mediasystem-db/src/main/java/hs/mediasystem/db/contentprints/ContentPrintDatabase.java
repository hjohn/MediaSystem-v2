package hs.mediasystem.db.contentprints;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.mediasystem.domain.stream.ContentID;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ContentPrintDatabase {
  private static final Map<String, Object> RESET_LAST_SEEN_TIME;

  static {
    Map<String, Object> map = new HashMap<>();

    map.put("lastseentime", null);

    RESET_LAST_SEEN_TIME = Collections.unmodifiableMap(map);
  }

  @Inject private Database database;

  public <T, U> Map<T, U> findAll(Function<ContentPrintRecord, T> keyMapper, Function<ContentPrintRecord, U> valueMapper) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      Map<T, U> map = new HashMap<>();

      tx.select(r -> map.put(keyMapper.apply(r), valueMapper.apply(r)), ContentPrintRecord.class);

      return map;
    }
  }

  public void forEach(Consumer<ContentPrintRecord> consumer) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      tx.select(consumer, ContentPrintRecord.class);
    }
  }

  public int findOrAdd(Long size, long lastModificationTime, byte[] hash) {
    try(Transaction tx = database.beginTransaction()) {
      ContentPrintRecord record = findStreamDataByHash(hash, size, lastModificationTime);

      if(record != null) {
        return record.getId();
      }

      record = new ContentPrintRecord();

      record.setSize(size);
      record.setLastModificationTime(lastModificationTime);
      record.setHash(hash);

      tx.insert(record);
      tx.commit();

      return record.getId();
    }
  }

  public void update(ContentID contentId, Long size, long lastModificationTime, byte[] hash) {  // used to update directories with latest signature
    try(Transaction tx = database.beginTransaction()) {
      ContentPrintRecord record = new ContentPrintRecord();

      record.setId(contentId.asInt());
      record.setSize(size);
      record.setLastModificationTime(lastModificationTime);
      record.setHash(hash);

      tx.update(record);
      tx.commit();
    }
  }

  private ContentPrintRecord findStreamDataByHash(byte[] hash, Long size, long lastModificationTime) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      if(size == null) {
        return tx.selectUnique(ContentPrintRecord.class, "hash = ? AND size IS NULL AND modtime = ?", hash, lastModificationTime);
      }

      return tx.selectUnique(ContentPrintRecord.class, "hash = ? AND size = ? AND modtime = ?", hash, size, lastModificationTime);
    }
  }

  public void markSeen(Set<ContentID> idsToMark) {
    try(Transaction tx = database.beginTransaction()) {
      long now = Instant.now().toEpochMilli();

      for(ContentID contentID : idsToMark) {
        tx.update("content_prints", contentID.asInt(), Map.of("lastseentime", now));
      }

      tx.commit();
    }
  }

  public void unmarkSeen(Set<ContentID> idsToUnmark) {
    try(Transaction tx = database.beginTransaction()) {
      for(ContentID contentID : idsToUnmark) {
        tx.update("content_prints", contentID.asInt(), RESET_LAST_SEEN_TIME);
      }

      tx.commit();
    }
  }
}
