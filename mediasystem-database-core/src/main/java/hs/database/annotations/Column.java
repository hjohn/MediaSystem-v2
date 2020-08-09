package hs.database.annotations;

import hs.database.core.DataTypeConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Column {
  String[] name() default {};
  Class<?> converterClass() default DefaultConverter.class;

  public static class DefaultConverter implements DataTypeConverter<Object, Object> {
    @Override
    public Object toStorageType(Object input) {
      if(input instanceof LocalDateTime) {
        return Date.from(((LocalDateTime)input).toInstant(ZoneOffset.UTC));
      }
      //else if(input instanceof String

      return input;
    }

    @Override
    public Object toJavaType(Object input, Class<?> type) {
      try {
        if(type.isEnum() && input instanceof String) {
          Method method = type.getMethod("valueOf", String.class);

          return method.invoke(null, input);
        }
        else if(input instanceof Blob) {
          Blob blob = (Blob)input;

          return blob.getBytes(1L, (int)blob.length());
        }
        else if(input instanceof Date) {
          Date date = (Date)input;

          return date.toLocalDate();
        }
        else if(input instanceof Timestamp) {
          Timestamp timestamp = (Timestamp)input;

          if(type.equals(LocalDateTime.class)) {
            return timestamp.toLocalDateTime();
          }
        }
        else if(type.equals(String.class) && input instanceof UUID) {
          return ((UUID)input).toString();
        }

        return input;
      }
      catch(InvocationTargetException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | SQLException e) {
        throw new RuntimeException("Exception during conversion to " + type + ": " + input, e);
      }
    }
  }
}
