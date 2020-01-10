package hs.mediasystem.db.base;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;

import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DescriptorDatabase {
  @Inject private Database database;

  public void forEach(Consumer<DescriptorRecord> consumer) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      tx.select(consumer, DescriptorRecord.class);
    }
  }

  public void store(DescriptorRecord record) {
    try(Transaction tx = database.beginTransaction()) {
      if(tx.selectUnique(DescriptorRecord.class, "identifier = ?", record.getIdentifier()) == null) {
        tx.insert(record);
      }
      else {
        tx.update(record);
      }

      tx.commit();
    }
  }

  public void delete(String id) {
    try(Transaction tx = database.beginTransaction()) {
      tx.delete(DescriptorRecord.class, id);
      tx.commit();
    }
  }
}
