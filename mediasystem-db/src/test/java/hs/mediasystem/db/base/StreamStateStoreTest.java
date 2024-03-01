package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.ContentID;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.int4.db.core.fluent.Row;
import org.int4.db.test.MockDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class StreamStateStoreTest {
  @Spy private MockDatabase database = new MockDatabase();
  @InjectMocks private StreamStateStore store;

  @BeforeEach
  public void beforeEach() {
    database.mockQuery("SELECT content_id, json FROM streamstate", List.of(
      Row.of(1, "{\"total-duration\":9469,\"watched\":true,\"last-watched-time\":\"2019-06-19T14:47:02.925460100\",\"resume-position\":152}".getBytes(StandardCharsets.UTF_8))
    ));
  }

  @Test
  void shouldStoreAndRetrieve() {
    store.forEach(ss -> {
      assertEquals(new ContentID(1), ss.getContentID());
      assertEquals(9469, ss.getProperties().get("total-duration"));
    });
  }
}
