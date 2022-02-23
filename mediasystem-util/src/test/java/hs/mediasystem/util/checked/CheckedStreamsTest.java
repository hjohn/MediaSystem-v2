package hs.mediasystem.util.checked;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CheckedStreamsTest {

  @Test
  void shouldThrowURISyntaxException() {
    assertThatThrownBy(() ->

      CheckedStreams.of(List.of(":", "A", "B"))
        .declaring(URISyntaxException.class)  // errors without this because URI::new throws checked exception
        .map(URI::new)
        .collect(Collectors.toList())

    ).isExactlyInstanceOf(URISyntaxException.class);
  }

  @Test
  void shouldNotThrowURISyntaxExceptionWhenInputValid() throws URISyntaxException {
    assertThat(

      CheckedStreams.of(List.of("http://hello.com", "A", "B"))
        .declaring(URISyntaxException.class)  // errors without this because URI::new throws checked exception
        .map(URI::new)
        .toList()

    ).containsExactly(new URI("http://hello.com"), new URI("A"), new URI("B"));
  }

  @Test
  void shouldNotThrowExceptionFromIntermediateOperations() {
    CheckedStreams.of(List.of(":", "A", "B"))
      .declaring(URISyntaxException.class)  // errors without this because URI::new throws checked exception
      .map(URI::new);
  }

  @Test
  void shouldSupportUptoThreeCheckedExceptions() throws URISyntaxException, IOException, SQLException {
    CheckedStreams.of(List.of(":", "A", "B"))
      .declaring(URISyntaxException.class)
      .declaring(IOException.class)
      .declaring(SQLException.class)
      .collect(Collectors.toList());
  }

  @Test
  void shouldWrapPartialStream() throws URISyntaxException {
    assertThat(

      CheckedStreams.of(List.of(":", "A", "B").stream().map(a -> a + a))
        .declaring(URISyntaxException.class)
        .collect(Collectors.toList())

    ).containsExactly("::", "AA", "BB");
  }
}
