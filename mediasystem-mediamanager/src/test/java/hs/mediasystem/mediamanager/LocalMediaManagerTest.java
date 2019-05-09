package hs.mediasystem.mediamanager;

import hs.ddif.core.Injector;
import hs.ddif.core.JustInTimeDiscoveryPolicy;
import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identification.MatchType;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService.Result;
import hs.mediasystem.scanner.api.Attribute;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.scanner.api.StreamPrint;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.StringURI;
import hs.mediasystem.util.Tuple;

import java.time.Instant;
import java.util.Collections;
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
  private static final StreamPrint STREAM_PRINT = new StreamPrint(new StreamID(999), 1024L, 0L, new byte[] {1, 2, 3});
  private static final StreamTags STREAM_TAGS = new StreamTags(Set.of("A", "B"));

  private static final MediaType MOVIE = MediaType.of("MOVIE");
  private static final MediaType SERIE = MediaType.of("SERIE");
  private static final MediaType EPISODE = MediaType.of("EPISODE");
  private static final DataSource DATA_SOURCE_1 = DataSource.instance(MOVIE, "D1");
  private static final DataSource DATA_SOURCE_2 = DataSource.instance(MOVIE, "D2");
  private static final DataSource DATA_SOURCE_3 = DataSource.instance(EPISODE, "D3");
  private static final DataSource DATA_SOURCE_4 = DataSource.instance(EPISODE, "D4");
  private static final DataSource DATA_SOURCE_5 = DataSource.instance(MOVIE, "D5");
  private static final DataSource DATA_SOURCE_6 = DataSource.instance(MOVIE, "D6");

  private final StreamPrint streamPrint1 = new StreamPrint(new StreamID(201), 1024L, 0L, new byte[] {1, 2, 3});
  private final StreamPrint streamPrint2 = new StreamPrint(new StreamID(201), 1024L, 0L, new byte[] {1, 2, 3});
  private final StreamPrint streamPrint3 = new StreamPrint(new StreamID(202), 2048L, 0L, new byte[] {3, 4, 5});

  @Mock private IdentificationService idServiceForDS1;
  @Mock private IdentificationService idServiceForDS2;
  @Mock private IdentificationService idService2;
  @Mock private IdentificationService idService3;
  @Mock private IdentificationService idService5;
  @Mock private IdentificationService serieIdService;
  @Mock private IdentificationService episodeIdService;
  @Mock private QueryService<Episode> queryService1;
  @Mock private QueryService<Movie> queryServiceForDS1;
  @Mock private QueryService<Movie> queryServiceForDS6;

  private StreamStore streamStore = new StreamStore();

  private LocalMediaManager db;

  @Before
  public void before() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(idServiceForDS1.getDataSource()).thenReturn(DATA_SOURCE_1);
    when(idServiceForDS2.getDataSource()).thenReturn(DATA_SOURCE_2);
    when(idService2.getDataSource()).thenReturn(DATA_SOURCE_3);
    when(idService3.getDataSource()).thenReturn(DATA_SOURCE_4);
    when(idService5.getDataSource()).thenReturn(DATA_SOURCE_6);
    when(serieIdService.getDataSource()).thenReturn(DataSource.instance(SERIE, "TMDB"));
    when(episodeIdService.getDataSource()).thenReturn(DataSource.instance(EPISODE, "TMDB"));

    when(queryService1.getDataSource()).thenReturn(DATA_SOURCE_3);
    when(queryServiceForDS1.getDataSource()).thenReturn(DATA_SOURCE_1);
    when(queryServiceForDS6.getDataSource()).thenReturn(DATA_SOURCE_6);

    Injector injector = new Injector(new JustInTimeDiscoveryPolicy());

    injector.registerInstance(idServiceForDS2);
    injector.registerInstance(idService2);
    injector.registerInstance(idService3);
    injector.registerInstance(idServiceForDS1);
    injector.registerInstance(idService5);
    injector.registerInstance(serieIdService);
    injector.registerInstance(episodeIdService);
    injector.registerInstance(queryService1);
    injector.registerInstance(queryServiceForDS1);
    injector.registerInstance(queryServiceForDS6);
    injector.registerInstance(streamStore);

    this.db = injector.getInstance(LocalMediaManager.class);
  }

  @Test
  public void shouldAddNewMediaAndIdentifyOnly() {
    ProductionIdentifier identifier = new ProductionIdentifier(DATA_SOURCE_1, "A1");
    Identifier identifier2 = new Identifier(DATA_SOURCE_2, "B1");
    Identifier identifier3 = new Identifier(DATA_SOURCE_5, "IMDB");

    Attributes attributes = Attributes.of(Attribute.TITLE, "Terminator, The", Attribute.YEAR, "2015");
    BasicStream stream = createMovie("file://parent/test", attributes);

    when(idServiceForDS2.identify(eq(stream)))
      .thenReturn(Tuple.of(identifier2, new Identification(MatchType.ID, 1.0, Instant.now())));

    db.put(stream, STREAM_TAGS, Set.of(
      Tuple.of(identifier, new Identification(MatchType.ID, 1.0, Instant.now()), Movies.create(identifier)),
      Tuple.of(identifier3, new Identification(MatchType.ID, 1.0, Instant.now()), null)
    ));

    db.incrementallyUpdateStream(stream.getId());

    assertTrue(streamStore.findIdentifiers(stream.getId()).contains(identifier2));
    assertTrue(streamStore.findDescriptorsAndIdentifications(stream.getId()).get(identifier2).b == null);

    verify(queryServiceForDS1, never()).query(any(Identifier.class));  // This Query result was provided, so should not be queried
  }

  @Test
  public void shouldAddNewMediaAndIdentifyAndQuery() {
    ProductionIdentifier identifier = new ProductionIdentifier(DATA_SOURCE_4, "A1");
    ProductionIdentifier identifier2 = new ProductionIdentifier(DATA_SOURCE_3, "B1");
    Episode episode1 = Episodes.create(identifier);
    Episode episode2 = Episodes.create(identifier2);

    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    BasicStream stream = createEpisode("file://parent/test", attributes);

    Identification identification = new Identification(MatchType.HASH, 0.99, Instant.now());

    when(idService2.identify(eq(stream)))
      .thenReturn(Tuple.of(identifier2, identification));

    when(queryService1.query(identifier2)).thenReturn(Result.of(episode2));

    db.put(stream, STREAM_TAGS, Set.of(
      Tuple.of(identifier, new Identification(MatchType.ID, 1.0, Instant.now()), episode1)
    ));

    db.incrementallyUpdateStream(stream.getId());

    assertTrue(streamStore.findIdentifiers(stream.getId()).contains(identifier2));
    assertEquals(Tuple.of(identification, episode2), streamStore.findDescriptorsAndIdentifications(stream.getId()).get(identifier2));

    verify(idService3, never()).identify(any(BasicStream.class));  // This Identifier provided, so should not be identified
  }

  @Test
  public void shouldReplaceDuplicate() {
    ProductionIdentifier identifier = new ProductionIdentifier(DATA_SOURCE_4, "A1");

    BasicStream stream = createEpisode("file://test", streamPrint1, Attributes.of(Attribute.TITLE, "Title"));
    BasicStream similarStream = createEpisode("file://moved/test", streamPrint2, Attributes.of(Attribute.TITLE, "Title"));
    BasicStream differentStream = createEpisode("file://test", streamPrint3, Attributes.of(Attribute.TITLE, "Title"));

    // Add the stream
    db.put(stream, STREAM_TAGS, Set.of(
      Tuple.of(identifier, new Identification(MatchType.ID, 1.0, Instant.now()), Episodes.create(identifier))
    ));

    // Should have 1 item
    assertEquals(1, streamStore.findStreams(EPISODE).size());

    // Add same stream
    db.put(stream, STREAM_TAGS, Set.of(
      Tuple.of(identifier, new Identification(MatchType.ID, 1.0, Instant.now()), Episodes.create(identifier))
    ));

    // Should still have 1 item
    assertEquals(1, streamStore.findStreams(EPISODE).size());

    // Add similar stream
    db.put(similarStream, STREAM_TAGS, Set.of(
      Tuple.of(identifier, new Identification(MatchType.ID, 1.0, Instant.now()), Episodes.create(identifier))
    ));

    // Should still have 1 item
    assertEquals(1, streamStore.findStreams(EPISODE).size());

    // Add really different stream
    db.put(differentStream, STREAM_TAGS, Set.of(
      Tuple.of(identifier, new Identification(MatchType.ID, 1.0, Instant.now()), Episodes.create(identifier))
    ));

    // Should have 2 item
    assertEquals(2, streamStore.findStreams(EPISODE).size());
  }

  @Test
  public void shouldRemoveMediaByStreamPrintIdentifier() {
    StreamPrint streamPrint1 = new StreamPrint(new StreamID(101), 1024L, 0L, new byte[] {1, 2, 3});
    StreamPrint streamPrint2 = new StreamPrint(new StreamID(101), 1024L, 0L, new byte[] {1, 2, 3});

    ProductionIdentifier identifier = new ProductionIdentifier(DATA_SOURCE_4, "A1");

    BasicStream stream = createEpisode("file://test", streamPrint1, Attributes.of(Attribute.TITLE, "Title"));
    BasicStream similarStream = createEpisode("file://moved/test", streamPrint2, Attributes.of(Attribute.TITLE, "Title"));

    // Add the stream
    db.put(stream, STREAM_TAGS, Set.of(
      Tuple.of(identifier, new Identification(MatchType.ID, 1.0, Instant.now()), Episodes.create(identifier))
    ));

    assertEquals(1, streamStore.findStreams(EPISODE).size());

    // Here the stream is removed, with the exact same input
    db.remove(stream);

    assertEquals(0, streamStore.findStreams(EPISODE).size());

    // Re-add it again
    db.put(stream, STREAM_TAGS, Set.of(
      Tuple.of(identifier, new Identification(MatchType.ID, 1.0, Instant.now()), Episodes.create(identifier))
    ));

    assertEquals(1, streamStore.findStreams(EPISODE).size());

    // Here the stream is removed, the fact that the StreamPrint differs in URL should have no effect.
    db.remove(similarStream);

    assertEquals(0, streamStore.findStreams(EPISODE).size());
  }

  @Test
  public void reidentifyShouldCreateIdentificationWithoutMatchingQueryService() {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    Identifier identifier = new Identifier(DATA_SOURCE_2, "12345");
    Identification identification = new Identification(MatchType.ID, 1.0, Instant.now());
    BasicStream stream = createMovie("file://parent/test", attributes);

    when(idServiceForDS2.identify(eq(stream))).thenReturn(Tuple.of(identifier, identification));

    db.put(stream, STREAM_TAGS, Set.of());

    MediaIdentification mi = db.reidentifyStream(stream.getId());

    assertEquals(3, mi.getResults().size());
    assertTrue(mi.getResults().contains(Exceptional.ofException(new UnknownStreamException(stream, idServiceForDS1))));
    assertTrue(mi.getResults().contains(Exceptional.ofException(new UnknownStreamException(stream, idService5))));
    assertTrue(mi.getResults().contains(Exceptional.of(Tuple.of(identifier, identification, null))));
  }

  @Test
  public void reidentifyShouldCreateIdentificationAndDescriptorAndRecursivelyAnotherDescriptor() {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    ProductionIdentifier identifier = new ProductionIdentifier(DATA_SOURCE_1, "12345");
    ProductionIdentifier identifier2 = new ProductionIdentifier(DATA_SOURCE_6, "abcdef");
    Identification identification = new Identification(MatchType.ID, 1.0, Instant.now());
    Identification identification2 = new Identification(MatchType.ID, 1.0, Instant.now());
    Movie movie1 = Movies.create(identifier);
    Movie movie2 = Movies.create(identifier2);

    BasicStream stream = createMovie("file://parent/test", attributes);

    when(idServiceForDS1.identify(eq(stream))).thenReturn(Tuple.of(identifier, identification));
    when(queryServiceForDS1.query(identifier)).thenReturn(QueryService.Result.of(movie1, Map.of(identifier2, identification2, identifier, identification)));  // Also returns original identification which should be skipped
    when(queryServiceForDS6.query(identifier2)).thenReturn(QueryService.Result.of(movie2, Map.of(identifier, identification)));  // Returns original identification which should be skipped

    db.put(stream, STREAM_TAGS, Set.of());

    MediaIdentification mi = db.reidentifyStream(stream.getId());

    assertEquals(4, mi.getResults().size());
    assertTrue(mi.getResults().contains(Exceptional.ofException(new UnknownStreamException(stream, idServiceForDS2))));
    assertTrue(mi.getResults().contains(Exceptional.ofException(new UnknownStreamException(stream, idService5))));
    assertTrue(mi.getResults().contains(Exceptional.of(Tuple.of(identifier, identification, movie1))));
    assertTrue(mi.getResults().contains(Exceptional.of(Tuple.of(identifier2, identification2, movie2))));
  }

  @Test
  public void incrementalIdentifyShouldNotIdentifyOrQueryExistingSources() {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    ProductionIdentifier identifier = new ProductionIdentifier(DATA_SOURCE_1, "12345");
    ProductionIdentifier identifier2 = new ProductionIdentifier(DATA_SOURCE_6, "abcdef");
    Identification identification = new Identification(MatchType.ID, 1.0, Instant.now());
    Identification identification2 = new Identification(MatchType.ID, 1.0, Instant.now());
    Movie movie1 = Movies.create(identifier);
    Movie movie2 = Movies.create(identifier2);

    BasicStream stream = createMovie("file://parent/test", attributes);

    when(queryServiceForDS1.query(identifier)).thenReturn(QueryService.Result.of(movie1));  // Also returns original identification which should be skipped

    streamStore.put(stream, STREAM_TAGS, Set.of(
      Tuple.of(identifier, identification, null),
      Tuple.of(identifier2, identification2, movie2)
    ));

    MediaIdentification mi = db.incrementallyUpdateStream(stream.getId());

    assertEquals(2, mi.getResults().size());
    assertTrue(mi.getResults().contains(Exceptional.of(Tuple.of(identifier, identification, movie1))));
    assertTrue(mi.getResults().contains(Exceptional.ofException(new UnknownStreamException(stream, idServiceForDS2))));
    verify(idServiceForDS1, never()).identify(any());
  }

  @Test
  public void reindentifyShouldHandleIdentifyException() {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");

    IllegalStateException illegalStateException = new IllegalStateException("oops");
    BasicStream stream = createMovie("file://parent/test", attributes);

    when(idServiceForDS2.identify(eq(stream))).thenThrow(illegalStateException);

    db.put(stream, STREAM_TAGS, Set.of());

    MediaIdentification mi = db.reidentifyStream(stream.getId());

    assertEquals(3, mi.getResults().size());
    assertTrue(mi.getResults().contains(Exceptional.ofException(new UnknownStreamException(stream, idServiceForDS1))));
    assertTrue(mi.getResults().contains(Exceptional.ofException(new UnknownStreamException(stream, idService5))));
    assertTrue(mi.getResults().contains(Exceptional.ofException(illegalStateException)));
  }

  @Test
  public void reindentifyShouldHandleQueryException() {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    Identifier identifier = new Identifier(DATA_SOURCE_1, "12345");
    Identification identification = new Identification(MatchType.ID, 1.0, Instant.now());

    IllegalStateException illegalStateException = new IllegalStateException("oops");
    BasicStream stream = createMovie("file://parent/test", attributes);

    when(idServiceForDS1.identify(eq(stream))).thenReturn(Tuple.of(identifier, identification));
    when(queryServiceForDS1.query(identifier)).thenThrow(illegalStateException);

    db.put(stream, STREAM_TAGS, Set.of());

    MediaIdentification mi = db.reidentifyStream(stream.getId());

    assertEquals(3, mi.getResults().size());
    assertTrue(mi.getResults().contains(Exceptional.ofException(new UnknownStreamException(stream, idServiceForDS2))));
    assertTrue(mi.getResults().contains(Exceptional.ofException(new UnknownStreamException(stream, idService5))));
    assertTrue(mi.getResults().contains(Exceptional.ofException(illegalStateException)));
  }


  private static BasicStream createMovie(String uri, Attributes attributes) {
    return new BasicStream(MediaType.of("MOVIE"), new StringURI(uri), STREAM_PRINT, attributes, Collections.emptyList());
  }

  private static BasicStream createEpisode(String uri, StreamPrint streamPrint, Attributes attributes) {
    return new BasicStream(MediaType.of("EPISODE"), new StringURI(uri), streamPrint, attributes, Collections.emptyList());
  }

  private static BasicStream createEpisode(String uri, Attributes attributes) {
    return createEpisode(uri, STREAM_PRINT, attributes);
  }
}
