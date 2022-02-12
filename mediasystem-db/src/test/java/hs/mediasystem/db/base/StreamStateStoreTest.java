package hs.mediasystem.db.base;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.mediasystem.domain.stream.ContentID;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StreamStateStoreTest {
  @Mock private Transaction tx;
  @Mock private Database database;
  @InjectMocks private StreamStateStore store;

  @SuppressWarnings("resource")
  @BeforeEach
  public void before() {
    when(database.beginReadOnlyTransaction()).thenReturn(tx);
  }

  @SuppressWarnings({"unchecked", "resource"})
  @Test
  void shouldStoreAndRetrieve() {
    StreamStateRecord record = new StreamStateRecord();

    record.setJson("{\"total-duration\":9469,\"watched\":true,\"last-watched-time\":\"2019-06-19T14:47:02.925460100\",\"resume-position\":152}".getBytes(StandardCharsets.UTF_8));
    record.setContentId(1);

    doAnswer(invocation -> {
      ((Consumer<StreamStateRecord>)invocation.getArgument(0)).accept(record);
      return null;
    }).when(tx).select(any(Consumer.class), eq(StreamStateRecord.class));

    store.forEach(ss -> {
      assertEquals(new ContentID(1), ss.getContentID());
      assertEquals(9469, ss.getProperties().get("total-duration"));
    });
  }

}
