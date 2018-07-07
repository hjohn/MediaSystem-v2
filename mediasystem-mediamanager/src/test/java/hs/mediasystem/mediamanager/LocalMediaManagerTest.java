package hs.mediasystem.mediamanager;

import hs.ddif.core.Injector;
import hs.ddif.core.JustInTimeDiscoveryPolicy;
import hs.mediasystem.ext.basicmediatypes.AbstractMediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.AbstractMediaStream;
import hs.mediasystem.ext.basicmediatypes.Attribute;
import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identification.MatchType;
import hs.mediasystem.ext.basicmediatypes.IdentificationService;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaRecord;
import hs.mediasystem.ext.basicmediatypes.MovieDescriptor;
import hs.mediasystem.ext.basicmediatypes.MovieStream;
import hs.mediasystem.ext.basicmediatypes.QueryService;
import hs.mediasystem.ext.basicmediatypes.QueryService.Result;
import hs.mediasystem.ext.basicmediatypes.StreamPrint;
import hs.mediasystem.ext.basicmediatypes.Type;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocalMediaManagerTest {
  private static final StreamPrint STREAM_PRINT = new StreamPrint(new StringURI("file://test"), 1024L, 0L, new byte[] {1, 2, 3}, 12345L);

  private static final Type MOVIE = Type.of("MOVIE");
  private static final Type EPISODE = Type.of("EPISODE");
  private static final DataSource DATA_SOURCE_1 = DataSource.instance(MOVIE, "D1");
  private static final DataSource DATA_SOURCE_2 = DataSource.instance(MOVIE, "D2");
  private static final DataSource DATA_SOURCE_3 = DataSource.instance(EPISODE, "D3");
  private static final DataSource DATA_SOURCE_4 = DataSource.instance(EPISODE, "D4");
  private static final DataSource DATA_SOURCE_5 = DataSource.instance(MOVIE, "D5");

  @Mock private IdentificationService<MovieStream> idService1;
  @Mock private IdentificationService<EpisodeStream> idService2;
  @Mock private IdentificationService<EpisodeStream> idService3;
  @Mock private QueryService<EpisodeDescriptor> queryService1;
  @Mock private QueryService<MovieDescriptor> queryService2;

  private LocalMediaManager db;

  @Before
  public void before() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(idService1.getDataSource()).thenReturn(DATA_SOURCE_2);
    when(idService2.getDataSource()).thenReturn(DATA_SOURCE_3);
    when(idService3.getDataSource()).thenReturn(DATA_SOURCE_4);
    when(queryService1.getDataSource()).thenReturn(DATA_SOURCE_3);
    when(queryService2.getDataSource()).thenReturn(DATA_SOURCE_1);

    Injector injector = new Injector(new JustInTimeDiscoveryPolicy());

    injector.registerInstance(idService1);
    injector.registerInstance(idService2);
    injector.registerInstance(idService3);
    injector.registerInstance(queryService1);
    injector.registerInstance(queryService2);

    this.db = injector.getInstance(LocalMediaManager.class);
  }

  @Test
  public void shouldAddNewMediaAndIdentifyOnly() throws InterruptedException {
    Identifier identifier = new Identifier(DATA_SOURCE_1, "A1");
    Identifier identifier2 = new Identifier(DATA_SOURCE_2, "B1");
    Identifier identifier3 = new Identifier(DATA_SOURCE_5, "IMDB");

    MovieStream stream = new MovieStream(STREAM_PRINT, Attributes.of(Attribute.TITLE, "Terminator, The", Attribute.YEAR, "2015"), new HashMap<Identifier, MediaRecord<MovieDescriptor>>() {{
      put(identifier, new MediaRecord<>(new Identification(identifier, MatchType.ID, 1.0), new MovieDescriptor(Productions.create(), null)));
      put(identifier3, new MediaRecord<>(new Identification(identifier3, MatchType.ID, 1.0), null));
    }});

    when(idService1.identify(eq(stream))).thenReturn(new Identification(identifier2, MatchType.ID, 1.0));

    db.add(stream);

    Thread.sleep(100);

    assertTrue(stream.getMediaRecords().containsKey(identifier2));
    assertTrue(stream.getMediaRecords().get(identifier2).getMediaDescriptor() == null);

    verify(queryService2, never()).query(any(Identifier.class));  // This Query result was provided, so should not be queried
  }

  @Test
  public void shouldAddNewMediaAndIdentifyAndQuery() throws InterruptedException {
    Identifier identifier = new Identifier(DATA_SOURCE_4, "A1");
    Identifier identifier2 = new Identifier(DATA_SOURCE_3, "B1");
    EpisodeDescriptor episodeDescriptor = new EpisodeDescriptor();

    EpisodeStream stream = new EpisodeStream(EPISODE, STREAM_PRINT, Attributes.of(Attribute.TITLE, "Title"), new HashMap<Identifier, MediaRecord<EpisodeDescriptor>>() {{
      put(identifier, new MediaRecord<>(new Identification(identifier, MatchType.ID, 1.0), new EpisodeDescriptor()));
    }});

    Identification identification = new Identification(identifier2, MatchType.HASH, 0.99);

    when(idService2.identify(eq(stream))).thenReturn(identification);

    when(queryService1.query(identifier2)).thenReturn(Result.of(episodeDescriptor));

    db.add(stream);

    Thread.sleep(100);

    assertTrue(stream.getMediaRecords().containsKey(identifier2));
    assertEquals(new MediaRecord<>(identification, episodeDescriptor), stream.getMediaRecords().get(identifier2));

    verify(idService3, never()).identify(any(EpisodeStream.class));  // This Identifier provided, so should not be identified
  }

  @Test
  public void shouldReplaceDuplicate() {
    StreamPrint streamPrint1 = new StreamPrint(new StringURI("file://test"), 1024L, 0L, new byte[] {1, 2, 3}, 12345L);
    StreamPrint streamPrint2 = new StreamPrint(new StringURI("file://moved/test"), 1024L, 0L, new byte[] {1, 2, 3}, 12345L);
    StreamPrint streamPrint3 = new StreamPrint(new StringURI("file://test"), 2048L, 0L, new byte[] {3, 4, 5}, 12345L);

    Identifier identifier = new Identifier(DATA_SOURCE_4, "A1");

    EpisodeStream stream = new EpisodeStream(EPISODE, streamPrint1, Attributes.of(Attribute.TITLE, "Title"), new HashMap<Identifier, MediaRecord<EpisodeDescriptor>>() {{
      put(identifier, new MediaRecord<>(new Identification(identifier, MatchType.ID, 1.0), new EpisodeDescriptor()));
    }});
    EpisodeStream similarStream = new EpisodeStream(EPISODE, streamPrint2, Attributes.of(Attribute.TITLE, "Title"), new HashMap<Identifier, MediaRecord<EpisodeDescriptor>>() {{
      put(identifier, new MediaRecord<>(new Identification(identifier, MatchType.ID, 1.0), new EpisodeDescriptor()));
    }});
    EpisodeStream differentStream = new EpisodeStream(EPISODE, streamPrint3, Attributes.of(Attribute.TITLE, "Title"), new HashMap<Identifier, MediaRecord<EpisodeDescriptor>>() {{
      put(identifier, new MediaRecord<>(new Identification(identifier, MatchType.ID, 1.0), new EpisodeDescriptor()));
    }});

    // Add the stream
    db.add(stream);

    // Should have 1 item
    assertEquals(1, db.findAllByType(EPISODE).size());

    // Add same stream
    db.add(stream);

    // Should still have 1 item
    assertEquals(1, db.findAllByType(EPISODE).size());

    // Add similar stream
    db.add(similarStream);

    // Should still have 1 item
    assertEquals(1, db.findAllByType(EPISODE).size());

    // Add really different stream
    db.add(differentStream);

    // Should have 2 item
    assertEquals(2, db.findAllByType(EPISODE).size());
  }

  @Test
  public void shouldRemoveMediaByStreamPrintIdentifier() {
    StreamPrint streamPrint1 = new StreamPrint(new StringURI("file://test"), 1024L, 0L, new byte[] {1, 2, 3}, 12345L);
    StreamPrint streamPrint2 = new StreamPrint(new StringURI("file://moved/test"), 1024L, 0L, new byte[] {1, 2, 3}, 12345L);

    Identifier identifier = new Identifier(DATA_SOURCE_4, "A1");

    EpisodeStream stream = new EpisodeStream(EPISODE, streamPrint1, Attributes.of(Attribute.TITLE, "Title"), new HashMap<Identifier, MediaRecord<EpisodeDescriptor>>() {{
      put(identifier, new MediaRecord<>(new Identification(identifier, MatchType.ID, 1.0), new EpisodeDescriptor()));
    }});
    EpisodeStream similarStream = new EpisodeStream(EPISODE, streamPrint2, Attributes.of(Attribute.TITLE, "Title"), new HashMap<Identifier, MediaRecord<EpisodeDescriptor>>() {{
      put(identifier, new MediaRecord<>(new Identification(identifier, MatchType.ID, 1.0), new EpisodeDescriptor()));
    }});

    // Add the stream
    db.add(stream);

    // Here the stream is removed, with the exact same input
    assertTrue(db.remove(stream));

    // Re-add it again
    db.add(stream);

    // Here the stream is removed, the fact that the StreamPrint differs in URL should have no effect.
    assertTrue(db.remove(similarStream));
  }

  private static class EpisodeStream extends AbstractMediaStream<EpisodeDescriptor> {
    public EpisodeStream(Type type, StreamPrint streamPrint, Attributes attributes, Map<Identifier, MediaRecord<EpisodeDescriptor>> mediaRecords) {
      super(type, null, streamPrint, attributes, mediaRecords);
    }
  }

  private static class EpisodeDescriptor extends AbstractMediaDescriptor {
  }
}
