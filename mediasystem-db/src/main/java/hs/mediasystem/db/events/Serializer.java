package hs.mediasystem.db.events;

/**
 * An interface which converts objects to and from byte arrays.
 *
 * @param <T> the object type
 */
public interface Serializer<T> {

  /**
   * Converts the given value to a byte array.
   *
   * @param value a value, cannot be {@code null}
   * @return a byte array, never {@code null}
   * @throws NullPointerException when {@code value} is {@code null}
   * @throws SerializerException when serialization failed
   */
  byte[] serialize(T value) throws SerializerException;

  /**
   * Converts the given serialized data to an object of type {@code T}.
   *
   * @param serialized a byte array produced by {@link Serializer#serialize(Object)}, cannot be {@code null}
   * @return an instance of type {@code T}, never {@code null}
   * @throws NullPointerException when {@code serialized} is {@code null}
   * @throws SerializerException when deserialization failed
   */
  T unserialize(byte[] serialized) throws SerializerException;
}
