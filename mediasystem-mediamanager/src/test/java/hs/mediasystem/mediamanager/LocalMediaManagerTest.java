package hs.mediasystem.mediamanager;

import hs.ddif.core.Injector;
import hs.ddif.core.JustInTimeDiscoveryPolicy;
import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identification.MatchType;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Type;
import hs.mediasystem.ext.basicmediatypes.scan.AbstractMediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.scan.AbstractMediaStream;
import hs.mediasystem.ext.basicmediatypes.scan.Attribute;
import hs.mediasystem.ext.basicmediatypes.scan.MediaRecord;
import hs.mediasystem.ext.basicmediatypes.scan.MovieStream;
import hs.mediasystem.ext.basicmediatypes.scan.StreamPrint;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService.Result;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.StringURI;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  private static final DataSource DATA_SOURCE_6 = DataSource.instance(MOVIE, "D6");

  @Mock private IdentificationService idService1;
  @Mock private IdentificationService idService2;
  @Mock private IdentificationService idService3;
  @Mock private IdentificationService idService4;
  @Mock private IdentificationService idService5;
  @Mock private QueryService<EpisodeDescriptor> queryService1;
  @Mock private QueryService<Movie> queryService2;
  @Mock private QueryService<Movie> queryService3;

  private LocalMediaManager db;

  @Before
  public void before() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(idService1.getDataSource()).thenReturn(DATA_SOURCE_2);
    when(idService2.getDataSource()).thenReturn(DATA_SOURCE_3);
    when(idService3.getDataSource()).thenReturn(DATA_SOURCE_4);
    when(idService4.getDataSource()).thenReturn(DATA_SOURCE_1);
    when(idService5.getDataSource()).thenReturn(DATA_SOURCE_6);
    when(queryService1.getDataSource()).thenReturn(DATA_SOURCE_3);
    when(queryService2.getDataSource()).thenReturn(DATA_SOURCE_1);
    when(queryService3.getDataSource()).thenReturn(DATA_SOURCE_6);

    Injector injector = new Injector(new JustInTimeDiscoveryPolicy());

    injector.registerInstance(idService1);
    injector.registerInstance(idService2);
    injector.registerInstance(idService3);
    injector.registerInstance(idService4);
    injector.registerInstance(idService5);
    injector.registerInstance(queryService1);
    injector.registerInstance(queryService2);
    injector.registerInstance(queryService3);

    this.db = injector.getInstance(LocalMediaManager.class);
  }

  @Test
  public void shouldAddNewMediaAndIdentifyOnly() throws InterruptedException {
    Identifier identifier = new Identifier(DATA_SOURCE_1, "A1");
    Identifier identifier2 = new Identifier(DATA_SOURCE_2, "B1");
    Identifier identifier3 = new Identifier(DATA_SOURCE_5, "IMDB");

    Attributes attributes = Attributes.of(Attribute.TITLE, "Terminator, The", Attribute.YEAR, "2015");
    MovieStream stream = new MovieStream(STREAM_PRINT, attributes, new HashMap<Identifier, MediaRecord<Movie>>() {{
      put(identifier, new MediaRecord<>(new Identification(identifier, MatchType.ID, 1.0), Movies.create()));
      put(identifier3, new MediaRecord<>(new Identification(identifier3, MatchType.ID, 1.0), null));
    }});

    when(idService1.identify(eq(attributes))).thenReturn(new Identification(identifier2, MatchType.ID, 1.0));

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

    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    EpisodeStream stream = new EpisodeStream(EPISODE, STREAM_PRINT, attributes, new HashMap<Identifier, MediaRecord<EpisodeDescriptor>>() {{
      put(identifier, new MediaRecord<>(new Identification(identifier, MatchType.ID, 1.0), new EpisodeDescriptor()));
    }});

    Identification identification = new Identification(identifier2, MatchType.HASH, 0.99);

    when(idService2.identify(eq(attributes))).thenReturn(identification);

    when(queryService1.query(identifier2)).thenReturn(Result.of(episodeDescriptor));

    db.add(stream);

    Thread.sleep(100);

    assertTrue(stream.getMediaRecords().containsKey(identifier2));
    assertEquals(new MediaRecord<>(identification, episodeDescriptor), stream.getMediaRecords().get(identifier2));

    verify(idService3, never()).identify(any(Attributes.class));  // This Identifier provided, so should not be identified
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

  @Test
  public void reidentifyShouldCreateIdentificationWithoutMatchingQueryService() {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    Identifier identifier = new Identifier(DATA_SOURCE_2, "12345");
    Identification identification = new Identification(identifier, MatchType.ID, 1.0);

    when(idService1.identify(eq(attributes))).thenReturn(identification);

    MovieStream mediaStream = new MovieStream(STREAM_PRINT, attributes, Collections.emptyMap());

    List<Exceptional<MediaRecord<Movie>>> results = db.reidentify(mediaStream);

    assertEquals(3, results.size());
    assertTrue(results.contains(Exceptional.ofException(new UnknownStreamException(mediaStream, idService4))));
    assertTrue(results.contains(Exceptional.ofException(new UnknownStreamException(mediaStream, idService5))));
    assertTrue(results.contains(Exceptional.of(new MediaRecord<Movie>(identification, null))));
  }

  @Test
  public void reidentifyShouldCreateIdentificationAndDescriptorAndRecursivelyAnotherDescriptor() {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    Identifier identifier = new Identifier(DATA_SOURCE_1, "12345");
    Identifier identifier2 = new Identifier(DATA_SOURCE_6, "abcdef");
    Identification identification = new Identification(identifier, MatchType.ID, 1.0);
    Identification identification2 = new Identification(identifier2, MatchType.ID, 1.0);
    Movie movie1 = Movies.create();
    Movie movie2 = Movies.create();

    when(idService4.identify(eq(attributes))).thenReturn(identification);
    when(queryService2.query(identifier)).thenReturn(QueryService.Result.of(movie1, Set.of(identification2, identification)));  // Also returns original identification which should be skipped
    when(queryService3.query(identifier2)).thenReturn(QueryService.Result.of(movie2, Set.of(identification)));  // Returns original identification which should be skipped

    MovieStream mediaStream = new MovieStream(STREAM_PRINT, attributes, Collections.emptyMap());

    List<Exceptional<MediaRecord<Movie>>> results = db.reidentify(mediaStream);

    assertEquals(4, results.size());
    assertTrue(results.contains(Exceptional.ofException(new UnknownStreamException(mediaStream, idService1))));
    assertTrue(results.contains(Exceptional.ofException(new UnknownStreamException(mediaStream, idService5))));
    assertTrue(results.contains(Exceptional.of(new MediaRecord<>(identification, movie1))));
    assertTrue(results.contains(Exceptional.of(new MediaRecord<>(identification2, movie2))));
  }

  @Test
  public void incrementalIdentifyShouldNotIdentifyOrQueryExistingSources() {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    Map<Identifier, MediaRecord<Movie>> map = new HashMap<>();
    Identifier identifier = new Identifier(DATA_SOURCE_1, "12345");
    Identifier identifier2 = new Identifier(DATA_SOURCE_6, "abcdef");
    Identification identification = new Identification(identifier, MatchType.ID, 1.0);
    Identification identification2 = new Identification(identifier2, MatchType.ID, 1.0);
    Movie movie1 = Movies.create();
    Movie movie2 = Movies.create();

    map.put(identifier, new MediaRecord<>(identification, null));      // Exists, but incomplete, expect it in results but should not call identificationService
    map.put(identifier2, new MediaRecord<>(identification2, movie2));  // Exists and is complete, donot expect it in results

    when(idService4.identify(eq(attributes))).thenReturn(identification);
    when(queryService2.query(identifier)).thenReturn(QueryService.Result.of(movie1));  // Also returns original identification which should be skipped

    MovieStream mediaStream = new MovieStream(STREAM_PRINT, attributes, map);

    List<Exceptional<MediaRecord<Movie>>> results = db.incrementalIdentify(mediaStream);

    assertEquals(2, results.size());
    assertTrue(results.contains(Exceptional.of(new MediaRecord<>(identification, movie1))));
    assertTrue(results.contains(Exceptional.ofException(new UnknownStreamException(mediaStream, idService1))));
    verify(idService4, never()).identify(any());
  }

  @Test
  public void reindentifyShouldHandleIdentifyException() {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");

    IllegalStateException illegalStateException = new IllegalStateException("oops");

    when(idService1.identify(eq(attributes))).thenThrow(illegalStateException);

    MovieStream mediaStream = new MovieStream(STREAM_PRINT, attributes, Collections.emptyMap());

    List<Exceptional<MediaRecord<Movie>>> results = db.reidentify(mediaStream);

    assertEquals(3, results.size());
    assertTrue(results.contains(Exceptional.ofException(new UnknownStreamException(mediaStream, idService4))));
    assertTrue(results.contains(Exceptional.ofException(new UnknownStreamException(mediaStream, idService5))));
    assertTrue(results.contains(Exceptional.ofException(illegalStateException)));
  }

  @Test
  public void reindentifyShouldHandleQueryException() {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    Identifier identifier = new Identifier(DATA_SOURCE_1, "12345");
    Identification identification = new Identification(identifier, MatchType.ID, 1.0);

    IllegalStateException illegalStateException = new IllegalStateException("oops");

    when(idService4.identify(eq(attributes))).thenReturn(identification);
    when(queryService2.query(identifier)).thenThrow(illegalStateException);

    MovieStream mediaStream = new MovieStream(STREAM_PRINT, attributes, Collections.emptyMap());

    List<Exceptional<MediaRecord<Movie>>> results = db.reidentify(mediaStream);

    assertEquals(3, results.size());
    assertTrue(results.contains(Exceptional.ofException(new UnknownStreamException(mediaStream, idService1))));
    assertTrue(results.contains(Exceptional.ofException(new UnknownStreamException(mediaStream, idService5))));
    assertTrue(results.contains(Exceptional.ofException(illegalStateException)));
  }

  private static class EpisodeStream extends AbstractMediaStream<EpisodeDescriptor> {
    public EpisodeStream(Type type, StreamPrint streamPrint, Attributes attributes, Map<Identifier, MediaRecord<EpisodeDescriptor>> mediaRecords) {
      super(type, null, streamPrint, attributes, mediaRecords);
    }
  }

  private static class EpisodeDescriptor extends AbstractMediaDescriptor {
  }
}
