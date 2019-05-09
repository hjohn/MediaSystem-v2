package hs.mediasystem.db;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DatabaseLocalMediaStore {
  @Inject private Database database;

  public List<LocalMedia> findAllActive() {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      return tx.select(LocalMedia.class, "deleteTime IS NULL");
    }
  }

  public Map<Integer, LocalMedia> findByScannerId(long scannerId) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      return tx.select(LocalMedia.class, "scannerId = ?", scannerId).stream()
        .collect(Collectors.toMap(LocalMedia::getStreamId, Function.identity()));
    }
  }

  public LocalMedia findById(int id) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      return tx.selectUnique(LocalMedia.class, "stream_id = ?", id);
    }
  }

  public void store(LocalMedia localMedia) {
    try(Transaction tx = database.beginTransaction()) {
      if(tx.selectUnique(LocalMedia.class, "stream_id = ?", localMedia.getStreamId()) != null) {
        tx.update(localMedia);
      }
      else {
        tx.insert(localMedia);
      }
      tx.commit();
    }
  }
}
