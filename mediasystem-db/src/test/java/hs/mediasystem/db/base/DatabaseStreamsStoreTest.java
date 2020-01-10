package hs.mediasystem.db.base;

import hs.mediasystem.db.base.CachedStream;
import hs.mediasystem.db.base.CachedStreamCodec;
import hs.mediasystem.db.base.DatabaseStreamStore;
import hs.mediasystem.db.base.StreamDatabase;
import hs.mediasystem.db.base.StreamRecord;
import hs.mediasystem.domain.stream.Attribute;
import hs.mediasystem.domain.stream.BasicStream;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.PostConstructCaller;
import hs.mediasystem.util.StringURI;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseStreamsStoreTest {

  @Mock private CachedStreamCodec codec;
  @Mock private StreamDatabase database;

  @InjectMocks private DatabaseStreamStore store;

  @BeforeEach
  void beforeEach() {
    MockitoAnnotations.initMocks(this);

    PostConstructCaller.call(store);
  }

  @Test
  void removeShouldIgnoreUnknownStreamID() {
    store.remove(new StreamID(1));
  }

  @Nested
  class AfterAddingElements {
    StreamID streamId1 = new StreamID(1);
    StreamID streamId2 = new StreamID(2);
    StreamID streamId3 = new StreamID(3);
    BasicStream stream1 = new BasicStream(MediaType.of("SERIE"), new StringURI("file://dir1"), streamId1, Attributes.of(Attribute.TITLE, "SerieTitle1"), Collections.emptyList());
    BasicStream stream2 = new BasicStream(MediaType.of("SERIE"), new StringURI("file://dir2"), streamId2, Attributes.of(Attribute.TITLE, "SerieTitle2"), Collections.emptyList());
    BasicStream stream3 = new BasicStream(MediaType.of("SERIE"), new StringURI("file://dir2"), streamId3, Attributes.of(Attribute.TITLE, "SerieTitle2"), Collections.emptyList());

    @BeforeEach
    void beforeEach() {
      when(codec.toRecord(any(CachedStream.class))).thenReturn(new StreamRecord());

      store.put(1, stream1);
      store.put(2, stream2);
      store.put(1, stream3);

      verify(database, times(3)).store(any(StreamRecord.class));
    }

    @Test
    void findByImportSourceIdShouldFindStreams() {
      assertEquals(2, store.findByImportSourceId(1).size());
      assertEquals(1, store.findByImportSourceId(2).size());
      assertEquals(stream1, store.findByImportSourceId(1).get(streamId1));
      assertEquals(stream2, store.findByImportSourceId(2).get(streamId2));
      assertEquals(stream3, store.findByImportSourceId(1).get(streamId3));
    }

    @Nested
    class AfterRemovingOneElement {
      @BeforeEach
      void beforeEach() {
        store.remove(stream3.getId());
      }

      @Test
      void findByImportSourceIdShouldFindLessStreams() {
        assertEquals(1, store.findByImportSourceId(1).size());
        assertEquals(1, store.findByImportSourceId(2).size());
        assertEquals(stream1, store.findByImportSourceId(1).get(streamId1));
        assertEquals(stream2, store.findByImportSourceId(2).get(streamId2));
      }

      @Nested
      class AfterReaddingRemovedElement {
        @BeforeEach
        void beforeEach() {
          store.put(1, stream3);
        }

        @Test
        void findByImportSourceIdShouldFindAllStreams() {
          assertEquals(2, store.findByImportSourceId(1).size());
          assertEquals(1, store.findByImportSourceId(2).size());
          assertEquals(stream1, store.findByImportSourceId(1).get(streamId1));
          assertEquals(stream2, store.findByImportSourceId(2).get(streamId2));
          assertEquals(stream3, store.findByImportSourceId(1).get(streamId3));
        }
      }
    }
  }
}
