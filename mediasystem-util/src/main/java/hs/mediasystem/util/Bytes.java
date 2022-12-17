package hs.mediasystem.util;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An immutable array of bytes with content based equals and hashCode.
 */
public final class Bytes {
  private final byte[] data;

  /**
   * Constructs a new {@link Bytes} instance with the given bytes.
   *
   * @param bytes an array of bytes to use as data, cannot be {@code null}
   * @return a {@link Bytes} instance, never {@code null}
   * @throws NullPointerException when {@code bytes} is {@code null}
   */
  public static Bytes of(byte... bytes) {
    return new Bytes(Objects.requireNonNull(bytes, "bytes"));
  }

  /**
   * Constructs a new {@link Bytes} instance with the given integers. Only the least significant 8 bits of each integer
   * is used for each byte, meaning that -1, 255 and 511 will all result in a byte value of -1.
   *
   * <p>This is a convenience method allowing for more readable input values.
   *
   * @param bytes an array of integers to use as data, cannot be {@code null}
   * @return a {@link Bytes} instance, never {@code null}
   * @throws NullPointerException when {@code bytes} is {@code null}
   */
  public static Bytes of(int... bytes) {
    return new Bytes(Objects.requireNonNull(bytes, "bytes"));
  }

  private Bytes(byte[] bytes) {
    this.data = bytes.clone();
  }

  private Bytes(int[] bytes) {
    this.data = new byte[bytes.length];

    for(int i = 0; i < bytes.length; i++) {
      this.data[i] = (byte)bytes[i];
    }
  }

  /**
   * Returns the number of bytes held in this instance.
   *
   * @return the number of bytes held in this instance, never negative
   */
  public int size() {
    return data.length;
  }

  /**
   * Gets the byte at the given offset.
   *
   * @param offset an offset, cannot be negative and must be smaller than {@link #size()}
   * @return the byte at the given offset
   */
  public byte get(int offset) {
    if(offset < 0 || offset >= size()) {
      throw new NoSuchElementException("expecting offset to be between 0 and " + (size() - 1) + ": " + offset);
    }

    return data[offset];
  }

  /**
   * Returns a copy of the bytes in this instance.
   *
   * @return a byte array, never {@code null}
   */
  public byte[] toArray() {
    return data.clone();
  }

  /**
   * Returns a read-only {@link ByteBuffer} of the bytes in this instance.
   *
   * @return a {@link ByteBuffer}, never {@code null}
   */
  public ByteBuffer asByteBuffer() {
    return ByteBuffer.wrap(data).asReadOnlyBuffer();
  }

  /**
   * Returns an {@link InputStream} of the bytes in this instance.
   *
   * @return an {@link InputStream}, never {@code null}
   */
  public InputStream asInputStream() {
    return new InputStream() {
      private int position;

      @Override
      public int read() {
        return (position < size()) ? (data[position++] & 0xff) : -1;
      }
    };
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    Bytes other = (Bytes)obj;

    return Arrays.equals(data, other.data);
  }

  @Override
  public String toString() {
    return Arrays.toString(data);
  }
}
