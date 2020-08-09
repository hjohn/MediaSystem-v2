package hs.database.core;

import hs.database.core.Database.Transaction;

import java.util.List;
import java.util.Map;

public interface RecordMapper<T> {
  String getTableName();
  List<String> getColumnNames();

  Map<String, Object> extractIds(T object);
  Map<String, Object> extractValues(T object);
  Map<String, Object> associateIds(Object... ids);

  void applyValues(Transaction transaction, Object object, Map<String, Object> values);
  void setGeneratedKey(T object, Object key);

  void invokeAfterLoadStore(Object object, Database database) throws DatabaseException;

  boolean isTransient(T object);
  boolean isIdGenerated();
  boolean hasIdColumn();
}
