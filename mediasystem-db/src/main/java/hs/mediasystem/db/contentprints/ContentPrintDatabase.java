package hs.mediasystem.db.contentprints;

import hs.mediasystem.domain.stream.ContentID;

import java.time.Instant;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.int4.db.core.Database;
import org.int4.db.core.Transaction;
import org.int4.db.core.fluent.Extractor;
import org.int4.db.core.fluent.Reflector;

@Singleton
public class ContentPrintDatabase {
  private static final Reflector<ContentPrintRecord> ALL = Reflector.of(ContentPrintRecord.class).withNames("id", "hash", "size", "modtime", "lastseentime", "creation_ms");
  private static final Extractor<ContentPrintRecord> EXCEPT_ID = ALL.excluding("id");

  @Inject private Database database;

  public void forEach(Consumer<ContentPrintRecord> consumer) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      tx."SELECT \{ALL} FROM content_prints"
        .map(ALL)
        .consume(consumer);
    }
  }

  public ContentPrintRecord findOrAdd(Long size, long lastModificationTime, byte[] hash) {
    try(Transaction tx = database.beginTransaction()) {
      ContentPrintRecord r = findStreamDataByHash(hash, size, lastModificationTime);

      if(r != null) {
        return r;
      }

      r = new ContentPrintRecord(null, hash, size, lastModificationTime, null, Instant.now().toEpochMilli());
      r = tx."INSERT INTO content_prints (\{EXCEPT_ID}) VALUES (\{r})"
        .mapGeneratedKeys()
        .asInt()
        .map(r::withId)
        .get();
      tx.commit();

      return r;
    }
  }

  public ContentPrintRecord update(ContentID contentId, Long size, long lastModificationTime, byte[] hash) {  // used to update directories with latest signature
    try(Transaction tx = database.beginTransaction()) {
      ContentPrintRecord r = tx."SELECT \{ALL} FROM content_prints WHERE id = \{contentId.asInt()}"
        .map(ALL)
        .get()
        .with(hash, size, lastModificationTime);

      tx."UPDATE content_prints SET \{EXCEPT_ID.entries(r)} WHERE id = \{r.id()}".execute();
      tx.commit();

      return r;
    }
  }

  private ContentPrintRecord findStreamDataByHash(byte[] hash, Long size, long lastModificationTime) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      if(size == null) {
        return tx."SELECT \{ALL} FROM content_prints WHERE hash = \{hash} AND size IS NULL AND modtime = \{lastModificationTime}"
          .map(ALL)
          .get();
      }

      return tx."SELECT \{ALL} FROM content_prints WHERE hash = \{hash} AND size = \{size} AND modtime = \{lastModificationTime}"
        .map(ALL)
        .get();
    }
  }

  public void markSeen(Set<ContentID> idsToMark) {
    try(Transaction tx = database.beginTransaction()) {
      long now = Instant.now().toEpochMilli();

      for(ContentID contentID : idsToMark) {
        tx."UPDATE content_prints SET lastseentime = \{now} WHERE id = \{contentID.asInt()}".execute();
      }

      tx.commit();
    }
  }

  public void unmarkSeen(Set<ContentID> idsToUnmark) {
    try(Transaction tx = database.beginTransaction()) {
      for(ContentID contentID : idsToUnmark) {
        tx."UPDATE content_prints SET lastseentime = NULL WHERE id = \{contentID.asInt()}".execute();
      }

      tx.commit();
    }
  }
}
