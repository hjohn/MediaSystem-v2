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
 * Serializer for sealed MediaSystem events.
 *
 * @param <T> the sealed event type
 */
public class SealedTypeSerializer<T> implements Serializer<T> {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
    .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
    .registerModule(new SealedClassesModule())
    .registerModule(new MediaSystemDomainModule())
    .registerModule(new BasicDataTypesModule())
    .registerModule(new JavaTimeModule())
    .registerModule(new Jdk8Module())
    .registerModule(new ParameterNamesModule(Mode.PROPERTIES));

  private final Class<T> sealedClass;

  /**
   * Constructs a new instance.
   *
   * @param sealedClass a sealed class, cannot be {@code null}
   * @throws NullPointerException when {@code sealedClass} is {@code null}
   * @throws IllegalArgumentException when {@code sealedClass} is not a sealed class
   */
  public SealedTypeSerializer(Class<T> sealedClass) {
    if(!Objects.requireNonNull(sealedClass, "sealedClass").isSealed()) {
      throw new IllegalArgumentException("Class is not sealed: " + sealedClass);
    }

    this.sealedClass = sealedClass;
  }

  @Override
  public byte[] serialize(T value) throws SerializerException {
    try {
      return OBJECT_MAPPER.writeValueAsBytes(Objects.requireNonNull(value, "value"));
    }
    catch(JsonProcessingException e) {
      throw new SerializerException("Value of type " + sealedClass + " is not serializable: " + value, e);
    }
  }

  @Override
  public T unserialize(byte[] serialized) throws SerializerException {
    try {
      return OBJECT_MAPPER.readValue(Objects.requireNonNull(serialized, "serialized"), sealedClass);
    }
    catch(IOException e) {
      throw new SerializerException("Serialized data cannot be unserialized for type " + sealedClass, e);
    }
  }
}
