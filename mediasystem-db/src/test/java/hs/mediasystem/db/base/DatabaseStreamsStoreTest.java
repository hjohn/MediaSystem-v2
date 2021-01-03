package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrint;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrintProvider;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.PostConstructCaller;

import java.net.URI;
import java.time.Instant;

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
  @Mock private ContentPrintProvider contentPrintProvider;

  @InjectMocks private DatabaseStreamStore store;

  @BeforeEach
  void beforeEach() {
    MockitoAnnotations.initMocks(this);

    PostConstructCaller.call(store);
  }

  @Test
  void removeShouldIgnoreUnknownStreamID() {
    store.remove(new StreamID(1, new ContentID(1), "Unknown"));
  }

  @Nested
  class AfterAddingElements {
    StreamID streamId1 = new StreamID(1, new ContentID(1), "SerieTitle1");
    StreamID streamId2 = new StreamID(2, new ContentID(2), "SerieTitle2");
    StreamID streamId3 = new StreamID(1, new ContentID(3), "SerieTitle3");
    Streamable sa1 = new Streamable(MediaType.SERIE, URI.create("file://dir1"), streamId1, null, Attributes.of(Attribute.TITLE, "SerieTitle1"));
    Streamable sa2 = new Streamable(MediaType.SERIE, URI.create("file://dir2"), streamId2, null, Attributes.of(Attribute.TITLE, "SerieTitle2"));
    Streamable sa3 = new Streamable(MediaType.SERIE, URI.create("file://dir2"), streamId3, null, Attributes.of(Attribute.TITLE, "SerieTitle2"));
    Instant now = Instant.now();

    @BeforeEach
    void beforeEach() {
      when(codec.toRecord(any(CachedStream.class))).thenReturn(new StreamRecord());
      when(contentPrintProvider.get(new ContentID(1))).thenReturn(new ContentPrint(new ContentID(1), 100L, 12345L, new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}, Instant.now()));
      when(contentPrintProvider.get(new ContentID(2))).thenReturn(new ContentPrint(new ContentID(2), 200L, 22345L, new byte[] {1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}, Instant.now()));
      when(contentPrintProvider.get(new ContentID(3))).thenReturn(new ContentPrint(new ContentID(3), 300L, 32345L, new byte[] {2, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}, Instant.now()));

      store.put(sa1, now);
      store.put(sa2, now);
      store.put(sa3, now);

      verify(database, times(3)).store(any(StreamRecord.class));
    }

    @Test
    void findByImportSourceIdShouldFindStreams() {
      assertEquals(2, store.findByImportSourceId(1).size());
      assertEquals(1, store.findByImportSourceId(2).size());
      assertEquals(sa1, store.findByImportSourceId(1).get(streamId1));
      assertEquals(sa2, store.findByImportSourceId(2).get(streamId2));
      assertEquals(sa3, store.findByImportSourceId(1).get(streamId3));
    }

    @Nested
    class AfterRemovingOneElement {
      @BeforeEach
      void beforeEach() {
        store.remove(sa3.getId());
      }

      @Test
      void findByImportSourceIdShouldFindLessStreams() {
        assertEquals(1, store.findByImportSourceId(1).size());
        assertEquals(1, store.findByImportSourceId(2).size());
        assertEquals(sa1, store.findByImportSourceId(1).get(streamId1));
        assertEquals(sa2, store.findByImportSourceId(2).get(streamId2));
      }

      @Nested
      class AfterReaddingRemovedElement {
        @BeforeEach
        void beforeEach() {
          store.put(sa3, now);
        }

        @Test
        void findByImportSourceIdShouldFindAllStreams() {
          assertEquals(2, store.findByImportSourceId(1).size());
          assertEquals(1, store.findByImportSourceId(2).size());
          assertEquals(sa1, store.findByImportSourceId(1).get(streamId1));
          assertEquals(sa2, store.findByImportSourceId(2).get(streamId2));
          assertEquals(sa3, store.findByImportSourceId(1).get(streamId3));
        }
      }
    }
  }
}
