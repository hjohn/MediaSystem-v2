package hs.database.core;

public interface DataTypeConverter<T, U> {
  U toStorageType(T input);
  T toJavaType(U input, Class<? extends T> type);
}
