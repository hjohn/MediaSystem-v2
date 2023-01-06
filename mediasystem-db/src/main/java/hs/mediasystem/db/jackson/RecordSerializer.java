package hs.mediasystem.db.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import hs.mediasystem.db.base.BasicDataTypesModule;
import hs.mediasystem.db.base.MediaSystemDomainModule;
import hs.mediasystem.db.events.Serializer;
import hs.mediasystem.db.events.SerializerException;

import java.io.IOException;
import java.util.Objects;

/**
 * Serializer for record based MediaSystem events.
 *
 * @param <T> the event type
 */
public class RecordSerializer<T> implements Serializer<T> {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
    .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
    .registerModule(new MediaSystemDomainModule())
    .registerModule(new BasicDataTypesModule())
    .registerModule(new JavaTimeModule())
    .registerModule(new Jdk8Module())
    .registerModule(new ParameterNamesModule(Mode.PROPERTIES));

  private final Class<T> cls;

  /**
   * Constructs a new instance.
   *
   * @param cls a class, cannot be {@code null}
   * @throws NullPointerException when {@code cls} is {@code null}
   */
  public RecordSerializer(Class<T> cls) {
    this.cls = Objects.requireNonNull(cls, "cls");
  }

  @Override
  public byte[] serialize(T value) throws SerializerException {
    try {
      return OBJECT_MAPPER.writeValueAsBytes(Objects.requireNonNull(value, "value"));
    }
    catch(JsonProcessingException e) {
      throw new SerializerException("Value of type " + cls + " is not serializable: " + value, e);
    }
  }

  @Override
  public T unserialize(byte[] serialized) throws SerializerException {
    try {
      return OBJECT_MAPPER.readValue(Objects.requireNonNull(serialized, "serialized"), cls);
    }
    catch(IOException e) {
      throw new SerializerException("Serialized data cannot be unserialized for type " + cls, e);
    }
  }
}
