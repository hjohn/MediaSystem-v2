package hs.mediasystem.util.checked;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class FlowTest {

  /**
   * This test just checks that the correct part of the chain to build a
   * stream is declaring the exception.  Intermediate operations never
   * throw exceptions and so shouldn't declare them.  Only the terminal
   * operations throw exceptions, as per the {@link Stream} documentation,
   * and so those operations need to declare the exception.
   */
  @Test
  void onlyTerminalOperationsShouldThrowExceptions() {
    // Does not throw an exception:
    Flow<String, IOException> s1 = Flow.forIOException(List.of("a", "b", "c").stream())
      .map(this::toLowerCase)  // accepts function without a throws clause
      .map(this::toUpperCase); // accepts function with a throws clause

    try {
      // Exception could be thrown here:
      assertEquals(List.of("A", "B", "C"), s1.collect(Collectors.toList()));
    }
    catch(IOException e) {
      fail("Did not expect an exception: " + e);
    }

    // Does not throw an exception:
    Flow<String, IOException> s2 = Flow.forIOException(List.of("aa", "b", "c").stream()).map(this::toUpperCase);

    // Exception is thrown here:
    assertThrows(IOException.class, () -> s2.collect(Collectors.toList()));

    // Does not throw an exception:
    Flow<String, IOException> s3 = Flow.forIOException(List.of("aa", "b", "c").stream()).map(this::toUpperCase);

    // Exception is thrown here:
    assertThrows(IOException.class, () -> s3.min(Comparator.naturalOrder()));

    // Does not throw an exception:
    Flow<String, IOException> s4 = Flow.forIOException(List.of("aa", "b", "c").stream()).map(this::toUpperCase);

    // Exception is thrown here:
    assertThrows(IOException.class, () -> s4.max(Comparator.naturalOrder()));
  }

  /**
   * Test function which throws a checked exception if input
   * was not exactly 1 character in length.
   *
   * @param input the input
   * @return the uppercased result
   * @throws IOException when input was not exactly 1 character in length
   */
  public String toUpperCase(String input) throws IOException {
    if(input.length() != 1) {
      throw new IOException("Can only convert one character at a time: " + input);
    }

    return input.toUpperCase();
  }

  public String toLowerCase(String input) {
    return input.toLowerCase();
  }
}
