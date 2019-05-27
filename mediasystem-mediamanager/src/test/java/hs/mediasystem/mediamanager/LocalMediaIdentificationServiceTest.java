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
import hs.mediasystem.scanner.api.Attribute;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.StringURI;
import hs.mediasystem.util.Tuple;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class LocalMediaIdentificationServiceTest {
  private static final StreamID STREAM_ID = new StreamID(999);

  private static final MediaType MOVIE = MediaType.of("MOVIE");
  private static final MediaType SERIE = MediaType.of("SERIE");
  private static final MediaType EPISODE = MediaType.of("EPISODE");
  private static final DataSource DATA_SOURCE_1 = DataSource.instance(MOVIE, "D1");
  private static final DataSource DATA_SOURCE_2 = DataSource.instance(MOVIE, "D2");
  private static final DataSource DATA_SOURCE_3 = DataSource.instance(EPISODE, "D3");
  private static final DataSource DATA_SOURCE_4 = DataSource.instance(EPISODE, "D4");
  private static final DataSource DATA_SOURCE_6 = DataSource.instance(MOVIE, "D6");

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

  private LocalMediaIdentificationService db;

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

    this.db = injector.getInstance(LocalMediaIdentificationService.class);
  }

  @Test
  public void reidentifyShouldCreateIdentificationWithoutMatchingQueryService() {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    Identifier identifier = new Identifier(DATA_SOURCE_2, "12345");
    Identification identification = new Identification(MatchType.ID, 1.0, Instant.now());
    BasicStream stream = createMovie("file://parent/test", attributes);

    when(idServiceForDS2.identify(eq(stream))).thenReturn(Tuple.of(identifier, identification));

    MediaIdentification mi = db.identify(stream, List.of("D1", "D2", "D5", "D6"));

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

    MediaIdentification mi = db.identify(stream, List.of("D1", "D2", "D5", "D6"));

    assertEquals(4, mi.getResults().size());
    assertTrue(mi.getResults().contains(Exceptional.ofException(new UnknownStreamException(stream, idServiceForDS2))));
    assertTrue(mi.getResults().contains(Exceptional.ofException(new UnknownStreamException(stream, idService5))));
    assertTrue(mi.getResults().contains(Exceptional.of(Tuple.of(identifier, identification, movie1))));
    assertTrue(mi.getResults().contains(Exceptional.of(Tuple.of(identifier2, identification2, movie2))));
  }

  @Test
  public void reindentifyShouldHandleIdentifyException() {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");

    IllegalStateException illegalStateException = new IllegalStateException("oops");
    BasicStream stream = createMovie("file://parent/test", attributes);

    when(idServiceForDS2.identify(eq(stream))).thenThrow(illegalStateException);

    MediaIdentification mi = db.identify(stream, List.of("D1", "D2", "D5", "D6"));

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

    MediaIdentification mi = db.identify(stream, List.of("D1", "D2", "D5", "D6"));

    assertEquals(3, mi.getResults().size());
    assertTrue(mi.getResults().contains(Exceptional.ofException(new UnknownStreamException(stream, idServiceForDS2))));
    assertTrue(mi.getResults().contains(Exceptional.ofException(new UnknownStreamException(stream, idService5))));
    assertTrue(mi.getResults().contains(Exceptional.ofException(illegalStateException)));
  }

  private static BasicStream createMovie(String uri, Attributes attributes) {
    return new BasicStream(MediaType.of("MOVIE"), new StringURI(uri), STREAM_ID, attributes, Collections.emptyList());
  }
}
