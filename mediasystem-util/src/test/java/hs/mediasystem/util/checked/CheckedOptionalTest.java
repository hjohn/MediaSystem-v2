package hs.mediasystem.util.checked;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class CheckedOptionalTest {

  @Test
  void test() {
    assertEquals("testtest", CheckedOptional.of("test").flatMap(this::normalFunction).orElseThrow());

    try {
      assertEquals("testtest", CheckedOptional.of("test").flatMap(this::throwingFunction).orElseThrow());
    }
    catch(IOException e) {
      fail("Not expected: ", e);
    }

    try {
      assertEquals("testtest", CheckedOptional.of("test").flatMap(this::doubleThrowingFunction).orElseThrow());
    }
    catch(Exception e) {
      fail("Not expected: ", e);
    }

    assertThrows(IOException.class, () -> CheckedOptional.of("").flatMap(this::throwingFunction).orElseThrow());
    assertThrows(IOException.class, () -> CheckedOptional.of("").flatMap(this::doubleThrowingFunction).orElseThrow());
    assertThrows(NoSuchMethodException.class, () -> CheckedOptional.of("boom").flatMap(this::doubleThrowingFunction).orElseThrow());
  }

  public CheckedOptional<String> normalFunction(String input) {
    return CheckedOptional.of(input + input);
  }

  public CheckedOptional<String> throwingFunction(String input) throws IOException {
    if(input.isEmpty()) {
      throw new IOException("eof");
    }

    return CheckedOptional.of(input + input);
  }

  public CheckedOptional<String> doubleThrowingFunction(String input) throws IOException, NoSuchMethodException {
    if(input.isEmpty()) {
      throw new IOException("eof");
    }
    if(input.equals("boom")) {
      throw new NoSuchMethodException("eof");
    }

    return CheckedOptional.of(input + input);
  }
}
