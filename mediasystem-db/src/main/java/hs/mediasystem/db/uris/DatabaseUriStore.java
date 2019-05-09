package hs.mediasystem.db.uris;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DatabaseUriStore {
  @Inject private Database database;

  public <T, U> Map<T, U> findAll(Function<UriRecord, T> keyMapper, Function<UriRecord, U> valueMapper) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      Map<T, U> map = new HashMap<>();

      tx.select(r -> map.put(keyMapper.apply(r), valueMapper.apply(r)), UriRecord.class);

      return map;
    }
  }

  public void store(String uri, int streamId) {
    try(Transaction tx = database.beginTransaction()) {
      UriRecord record = tx.selectUnique(UriRecord.class, "uri = ?", uri);

      if(record == null) {
        record = new UriRecord();
      }

      record.setUri(uri);
      record.setStreamId(streamId);

      tx.merge(record);
      tx.commit();
    }
  }

  public List<String> findUris(int streamId) {
    try(Transaction tx = database.beginReadOnlyTransaction()) {
      return tx.select(UriRecord.class, UriRecord::getUri, "stream_id = ?", streamId);
    }
  }
}
