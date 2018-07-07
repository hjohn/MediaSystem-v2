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

  public Map<String, LocalMedia> findByScannerId(long scannerId) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      return tx.select(LocalMedia.class, "scannerId = ?", scannerId).stream()
        .collect(Collectors.toMap(LocalMedia::getId, Function.identity()));
    }
  }

  public LocalMedia findById(String id) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      return tx.selectUnique(LocalMedia.class, "id = ?", id);
    }
  }

  public void store(LocalMedia localMedia) {
    try(Transaction tx = database.beginTransaction()) {
      if(tx.selectUnique(LocalMedia.class, "id = ?", localMedia.getId()) != null) {
        tx.update(localMedia);
      }
      else {
        tx.insert(localMedia);
      }
      tx.commit();
    }
  }
}
