package hs.database.core;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class Record implements Map<String, Object> {
  private final Object[] data;
  private final FieldMapper fieldMapper;

  public Record(Object[] data, FieldMapper fieldMapper) {
    this.data = data;
    this.fieldMapper = fieldMapper;
  }

  public Object get(String fieldName) {
    return data[fieldMapper.get(fieldName)];
  }

  public String getString(String fieldName) {
    Object object = get(fieldName);

    return object == null ? null : object.toString();
  }

  public Boolean getBoolean(String fieldName) {
    return (Boolean)get(fieldName);
  }

  public Integer getInteger(String fieldName) {
    return (Integer)get(fieldName);
  }

  public Long getLong(String fieldName) {
    return (Long)get(fieldName);
  }

  public Float getFloat(String fieldName) {
    return (Float)get(fieldName);
  }

  public byte[] getBytes(String fieldName) {
    return (byte[])get(fieldName);
  }

  public Date getDate(String fieldName) {
    return (Date)get(fieldName);
  }

  public LocalDate getLocalDate(String fieldName) {
    return ((java.sql.Date)get(fieldName)).toLocalDate();
  }

  public LocalDateTime getLocalDateTime(String fieldName) {
    return ((Timestamp)get(fieldName)).toLocalDateTime();
  }

  @Override
  public int size() {
    return data.length;
  }

  @Override
  public boolean isEmpty() {
    return data.length == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object get(Object key) {
    return get((String) key);
  }

  @Override
  public Object put(String key, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends String, ? extends Object> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<String> keySet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<Object> values() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<java.util.Map.Entry<String, Object>> entrySet() {
    throw new UnsupportedOperationException();
  }
}
