package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.PostConstructCaller;
import hs.mediasystem.util.StringURI;

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
  void removeShouldIgnoreUnknownContentID() {
    store.remove(new ContentID(1));
  }

  @Nested
  class AfterAddingElements {
    ContentID contentId1 = new ContentID(1);
    ContentID contentId2 = new ContentID(2);
    ContentID contentId3 = new ContentID(3);
    Streamable sa1 = new Streamable(MediaType.of("SERIE"), new StringURI("file://dir1"), contentId1, null, Attributes.of(Attribute.TITLE, "SerieTitle1"));
    Streamable sa2 = new Streamable(MediaType.of("SERIE"), new StringURI("file://dir2"), contentId2, null, Attributes.of(Attribute.TITLE, "SerieTitle2"));
    Streamable sa3 = new Streamable(MediaType.of("SERIE"), new StringURI("file://dir2"), contentId3, null, Attributes.of(Attribute.TITLE, "SerieTitle2"));

    @BeforeEach
    void beforeEach() {
      when(codec.toRecord(any(CachedStream.class))).thenReturn(new StreamRecord());

      store.put(1, sa1);
      store.put(2, sa2);
      store.put(1, sa3);

      verify(database, times(3)).store(any(StreamRecord.class));
    }

    @Test
    void findByImportSourceIdShouldFindStreams() {
      assertEquals(2, store.findByImportSourceId(1).size());
      assertEquals(1, store.findByImportSourceId(2).size());
      assertEquals(sa1, store.findByImportSourceId(1).get(contentId1));
      assertEquals(sa2, store.findByImportSourceId(2).get(contentId2));
      assertEquals(sa3, store.findByImportSourceId(1).get(contentId3));
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
        assertEquals(sa1, store.findByImportSourceId(1).get(contentId1));
        assertEquals(sa2, store.findByImportSourceId(2).get(contentId2));
      }

      @Nested
      class AfterReaddingRemovedElement {
        @BeforeEach
        void beforeEach() {
          store.put(1, sa3);
        }

        @Test
        void findByImportSourceIdShouldFindAllStreams() {
          assertEquals(2, store.findByImportSourceId(1).size());
          assertEquals(1, store.findByImportSourceId(2).size());
          assertEquals(sa1, store.findByImportSourceId(1).get(contentId1));
          assertEquals(sa2, store.findByImportSourceId(2).get(contentId2));
          assertEquals(sa3, store.findByImportSourceId(1).get(contentId3));
        }
      }
    }
  }
}
