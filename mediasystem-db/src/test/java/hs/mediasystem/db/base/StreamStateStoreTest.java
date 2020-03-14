package hs.mediasystem.db.base;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.mediasystem.domain.stream.StreamID;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class StreamStateStoreTest {
  @Mock private Transaction tx;
  @Mock private Database database;
  @InjectMocks private StreamStateStore store;

  @SuppressWarnings("resource")
  @BeforeEach
  public void before() {
    MockitoAnnotations.initMocks(this);

    when(database.beginReadOnlyTransaction()).thenReturn(tx);
  }

  @SuppressWarnings({"unchecked", "resource"})
  @Test
  void shouldStoreAndRetrieve() {
    StreamStateRecord record = new StreamStateRecord();

    record.setJson("{\"total-duration\":9469,\"watched\":true,\"last-watched-time\":\"2019-06-19T14:47:02.925460100\",\"resume-position\":152}".getBytes(StandardCharsets.UTF_8));
    record.setStreamId(1);

    doAnswer(invocation -> {
      ((Consumer<StreamStateRecord>)invocation.getArgument(0)).accept(record);
      return null;
    }).when(tx).select(any(Consumer.class), eq(StreamStateRecord.class));

    store.forEach(ss -> {
      assertEquals(new StreamID(1), ss.getStreamID());
      assertEquals(9469, ss.getProperties().get("total-duration"));
    });
  }

}
