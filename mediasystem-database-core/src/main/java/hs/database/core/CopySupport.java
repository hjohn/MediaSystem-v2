package hs.database.core;

import hs.database.core.Database.Transaction;

import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import org.postgresql.copy.CopyManager;
import org.postgresql.copy.CopyOut;

public class CopySupport {
  private static record FieldType(int length, BiFunction<ByteBuffer, Integer, Object> extractor) {}

  private static final Map<Class<?>, FieldType> FIELD_TYPES = Map.of(
    Long.class, new FieldType(8, (buf, len) -> buf.getLong()),
    Integer.class, new FieldType(4, (buf, len) -> buf.getInt()),
    byte[].class, new FieldType(-1, (buf, len) -> {
      byte[] data = new byte[len];

      buf.get(data);

      return data;
    })
  );

  static <T> List<T> copyAll(Transaction tx, CopyManager copyManager, Mapper<T> mapper, String sql, List<Class<?>> columnTypes) throws SQLException {
    Objects.requireNonNull(copyManager, "copyManager");
    Objects.requireNonNull(mapper, "mapper");
    Objects.requireNonNull(sql, "sql");
    Objects.requireNonNull(columnTypes, "columnTypes");

    if(columnTypes.isEmpty()) {
      throw new IllegalArgumentException("columnTypes cannot be empty");
    }

    long nanos = System.nanoTime();
    long bytesRead = 0;

    String fullSql = "COPY (" + sql + ") TO STDOUT WITH (FORMAT binary)";
    CopyOut copyOut = copyManager.copyOut(fullSql);
    List<T> decoded = new ArrayList<>();
    Object[] raw = new Object[columnTypes.size()];
    boolean headerSkipped = false;

    for(;;) {
      byte[] data = copyOut.readFromCopy();

      if(data == null) {
        break;
      }

      ByteBuffer buf = ByteBuffer.wrap(data);  // network byte order is default, so good

      bytesRead += data.length;

      if(!headerSkipped) {
        headerSkipped = true;
        buf.position(15);

        int extensionSize = buf.getInt();

        buf.position(15 + 4 + extensionSize);
      }

      int fieldCount = buf.getShort();

      if(fieldCount == -1) {
        continue;
      }

      if(fieldCount != columnTypes.size()) {
        throw new IllegalStateException("Unexpected field count: " + fieldCount);
      }

      for(int i = 0; i < columnTypes.size(); i++) {
        Class<?> columnType = columnTypes.get(i);
        FieldType fieldType = FIELD_TYPES.get(columnType);
        int fieldLength = buf.getInt();

        if(fieldType == null) {
          throw new IllegalStateException("Unsupported columnType: " + columnType);
        }

        if(fieldLength == -1) {
          raw[i] = null;
        }
        else {
          if(fieldType.length() >= 0 && fieldLength != fieldType.length()) {
            throw new IllegalStateException("Unexpected field length " + fieldLength + "; expected " + fieldType.length() + " for " + columnType);
          }

          raw[i] = fieldType.extractor().apply(buf, fieldLength);
        }
      }

      if(buf.remaining() != 0) {
        throw new IllegalStateException("Unexpected additional field encountered");
      }

      try {
        decoded.add(mapper.map(raw));
      }
      catch(Throwable e) {
        throw new IllegalStateException("Mapper threw exception for input: " + Arrays.toString(data), e);
      }
    }

    nanos = System.nanoTime() - nanos;

    StatementLogger.log(tx, fullSql, decoded.size(), bytesRead, nanos);

    return decoded;
  }
}
