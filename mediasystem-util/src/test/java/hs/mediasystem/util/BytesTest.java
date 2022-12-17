package hs.mediasystem.util;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BytesTest {
  private final byte[] data = new byte[] {1, 2, 3};
  private final Bytes bytes = Bytes.of(data);

  @Test
  void shouldBeImmutable() {
    assertThat(bytes.toArray()).isEqualTo(data);

    data[0] = 42;

    assertThat(bytes.toArray()).isNotEqualTo(data);
  }

  @Test
  void getShouldReturnIndividualBytes() {
    assertThat(bytes.get(0)).isEqualTo((byte)1);
    assertThat(bytes.get(1)).isEqualTo((byte)2);
    assertThat(bytes.get(2)).isEqualTo((byte)3);

    assertThatThrownBy(() -> bytes.get(-1)).isInstanceOf(NoSuchElementException.class);
    assertThatThrownBy(() -> bytes.get(3)).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void shouldProvideInputStream() throws IOException {
    try(InputStream inputStream = bytes.asInputStream()) {
      assertThat(inputStream.read()).isEqualTo(1);
      assertThat(inputStream.read()).isEqualTo(2);
      assertThat(inputStream.read()).isEqualTo(3);
      assertThat(inputStream.read()).isEqualTo(-1);
      assertThat(inputStream.read()).isEqualTo(-1);
      assertThat(inputStream.read()).isEqualTo(-1);
    }
  }

  @Test
  void shouldProvideByteBuffer() {
    ByteBuffer bb = bytes.asByteBuffer();

    assertThat(bb.get()).isEqualTo((byte)1);
    assertThat(bb.get()).isEqualTo((byte)2);
    assertThat(bb.get()).isEqualTo((byte)3);
    assertThatThrownBy(() -> bb.get()).isExactlyInstanceOf(BufferUnderflowException.class);
  }

  @Test
  void shouldNotAllowModificationToByteBuffer() {
    ByteBuffer bb = bytes.asByteBuffer();

    assertThatThrownBy(() -> bb.put((byte)42)).isExactlyInstanceOf(ReadOnlyBufferException.class);
  }

  @Test
  void staticConstructorWithIntegersShouldKeepOnlyTheLeastSignificantEightBits() {
    assertThat(Bytes.of(-1, 255, 511)).isEqualTo(Bytes.of((byte)-1, (byte)-1, (byte)-1));
    assertThat(Bytes.of(1, 257, 513)).isEqualTo(Bytes.of((byte)1, (byte)1, (byte)1));
  }

  @Test
  void shouldHaveNiceToString() {
    assertThat(bytes.toString()).isEqualTo("[1, 2, 3]");
  }

  @Test
  void shouldRespectEqualsAndHashcodeContract() {
    EqualsVerifier.forClass(Bytes.class).verify();
  }
}
