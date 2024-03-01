package hs.mediasystem.db.extract;

import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.int4.db.core.Database;
import org.int4.db.core.Transaction;
import org.int4.db.core.fluent.Extractor;
import org.int4.db.core.fluent.Reflector;

@Singleton
public class StreamMetaDataDatabase {
  private static final Reflector<StreamMetaDataRecord> ALL = Reflector.of(StreamMetaDataRecord.class).withNames("content_id", "modtime", "version", "json");
  private static final Extractor<StreamMetaDataRecord> EXCEPT_CONTENT_ID = ALL.excluding("content_id");
  private static final Extractor<StreamMetaDataRecord> KEY = ALL.only("content_id");

  @Inject private Database database;

  public void forEach(Consumer<StreamMetaDataRecord> consumer) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      tx."SELECT \{ALL} FROM stream_metadata"
        .map(ALL)
        .consume(consumer);
    }
  }

  public void store(StreamMetaDataRecord r) {
    try(Transaction tx = database.beginTransaction()) {
      if(tx."SELECT COUNT(*) FROM stream_metadata WHERE \{KEY.entries(r)}".asInt().get() == 0) {
        tx."INSERT INTO stream_metadata (\{ALL}) VALUES (\{r})".execute();
      }
      else {
        tx."UPDATE stream_metadata SET \{EXCEPT_CONTENT_ID.entries(r)} WHERE \{KEY.entries(r)}".execute();
      }

      tx.commit();
    }
  }

  public void delete(int id) {
    try(Transaction tx = database.beginTransaction()) {
      tx."DELETE FROM stream_metadata WHERE content_id = \{id}".execute();
      tx.commit();
    }
  }

  public byte[] readSnapshot(int id, int snapshotIndex) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      return tx."SELECT image FROM stream_metadata_snapshots WHERE content_id = \{id} AND index = \{snapshotIndex}"
        .asBytes().getOptional()
        .orElse(null);
    }
  }

  public boolean existsSnapshot(int id, int snapshotIndex) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      return tx."SELECT COUNT(*) FROM stream_metadata_snapshots WHERE content_id = \{id} AND index = \{snapshotIndex}"
        .asInt().get() > 0;
    }
  }

  public void storeImage(int contentId, int index, byte[] image) {
    try(Transaction tx = database.beginTransaction()) {
      tx."INSERT INTO stream_metadata_snapshots (content_id, index, image) VALUES (\{contentId}, \{index}, \{image}".execute();
      tx.commit();
    }
  }
}
