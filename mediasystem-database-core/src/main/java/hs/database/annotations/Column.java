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
      if(input instanceof LocalDateTime ldt) {
        return Date.from(ldt.toInstant(ZoneOffset.UTC));
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
        if(input instanceof Blob b) {
          return b.getBytes(1L, (int)b.length());
        }
        if(input instanceof Date d) {
          return d.toLocalDate();
        }
        if(type.equals(LocalDateTime.class) && input instanceof Timestamp t) {
          return t.toLocalDateTime();
        }
        if(type.equals(String.class) && input instanceof UUID u) {
          return u.toString();
        }

        return input;
      }
      catch(InvocationTargetException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | SQLException e) {
        throw new RuntimeException("Exception during conversion to " + type + ": " + input, e);
      }
    }
  }
}
