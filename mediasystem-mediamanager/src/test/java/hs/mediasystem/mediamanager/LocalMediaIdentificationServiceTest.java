package hs.mediasystem.mediamanager;

import hs.ddif.core.Injector;
import hs.ddif.core.JustInTimeDiscoveryPolicy;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.MatchType;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.BasicStream;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.StringURI;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
  private static final DataSource DATA_SOURCE_5 = DataSource.instance(MOVIE, "D5");
  private static final DataSource DATA_SOURCE_6 = DataSource.instance(MOVIE, "D6");

  @Mock private IdentificationService idServiceForDS1;
  @Mock private IdentificationService idServiceForDS2;
  @Mock private IdentificationService idServiceForDS3;
  @Mock private IdentificationService idServiceForDS4;
  @Mock private IdentificationService idServiceForDS6;
  @Mock private IdentificationService serieIdService;
  @Mock private IdentificationService episodeIdService;
  @Mock private QueryService queryServiceForDS1;
  @Mock private QueryService queryServiceForDS3;
  @Mock private QueryService queryServiceForDS6;

  private LocalMediaIdentificationService db;

  @Before
  public void before() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(idServiceForDS1.getDataSource()).thenReturn(DATA_SOURCE_1);
    when(idServiceForDS2.getDataSource()).thenReturn(DATA_SOURCE_2);
    when(idServiceForDS3.getDataSource()).thenReturn(DATA_SOURCE_3);
    when(idServiceForDS4.getDataSource()).thenReturn(DATA_SOURCE_4);
    when(idServiceForDS6.getDataSource()).thenReturn(DATA_SOURCE_6);
    when(serieIdService.getDataSource()).thenReturn(DataSource.instance(SERIE, "TMDB"));
    when(episodeIdService.getDataSource()).thenReturn(DataSource.instance(EPISODE, "TMDB"));

    when(queryServiceForDS1.getDataSource()).thenReturn(DATA_SOURCE_1);
    when(queryServiceForDS3.getDataSource()).thenReturn(DATA_SOURCE_3);
    when(queryServiceForDS6.getDataSource()).thenReturn(DATA_SOURCE_6);

    Injector injector = new Injector(new JustInTimeDiscoveryPolicy());

    injector.registerInstance(idServiceForDS1);
    injector.registerInstance(idServiceForDS2);
    injector.registerInstance(idServiceForDS3);
    injector.registerInstance(idServiceForDS4);
    injector.registerInstance(idServiceForDS6);
    injector.registerInstance(serieIdService);
    injector.registerInstance(episodeIdService);
    injector.registerInstance(queryServiceForDS1);
    injector.registerInstance(queryServiceForDS3);
    injector.registerInstance(queryServiceForDS6);

    this.db = injector.getInstance(LocalMediaIdentificationService.class);
  }

  @Test
  public void reidentifyShouldCreateIdentificationWithoutMatchingQueryService() {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    Identifier identifier1 = new Identifier(DATA_SOURCE_1, "12345");
    Identifier identifier2 = new Identifier(DATA_SOURCE_2, "12345");
    Match match = new Match(MatchType.ID, 1.0f, Instant.now());
    BasicStream stream = createMovie("file://parent/test", attributes);

    when(idServiceForDS1.identify(eq(stream))).thenReturn(Map.of(stream.getId(), new Identification(identifier1, match)));
    when(idServiceForDS2.identify(eq(stream))).thenReturn(Map.of(stream.getId(), new Identification(identifier2, match)));
    when(queryServiceForDS1.query(eq(identifier1))).thenReturn(Movies.create());

    MediaIdentification mi1 = db.identify(stream, "D1");  // id service available and query works
    Exceptional<MediaIdentification> mi2 = Exceptional.from(() -> db.identify(stream, "D2"));  // id service available and returns result, but query fails
    Exceptional<MediaIdentification> mi5 = Exceptional.from(() -> db.identify(stream, "D5"));  // no id service at all

    assertEquals(stream, mi1.getStream());
    assertEquals(Map.of(stream.getId(), new Identification(identifier1, match)), mi1.getIdentifications());
    assertNotNull(mi1.getDescriptor());
    assertEquals(Exceptional.ofException(new UnknownDataSourceException(DATA_SOURCE_2)), mi2);
    assertEquals(Exceptional.ofException(new UnknownDataSourceException(DATA_SOURCE_5)), mi5);
  }

  @Test
  public void reindentifyShouldHandleIdentifyException() {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");

    IllegalStateException illegalStateException = new IllegalStateException("oops");
    BasicStream stream = createMovie("file://parent/test", attributes);

    when(idServiceForDS2.identify(eq(stream))).thenThrow(illegalStateException);

    MediaIdentification mi1 = db.identify(stream, "D1");  // id service available, but unable to identify
    Exceptional<MediaIdentification> mi2 = Exceptional.from(() -> db.identify(stream, "D2"));  // id service available, but throws exception
    Exceptional<MediaIdentification> mi5 = Exceptional.from(() -> db.identify(stream, "D5"));  // no id service at all

    assertEquals(stream, mi1.getStream());
    assertEquals(Map.of(), mi1.getIdentifications());
    assertNull(mi1.getDescriptor());
    assertEquals(Exceptional.ofException(illegalStateException), mi2);
    assertEquals(Exceptional.ofException(new UnknownDataSourceException(DATA_SOURCE_5)), mi5);
  }

  @Test
  public void reindentifyShouldHandleQueryException() {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    Identifier identifier = new Identifier(DATA_SOURCE_1, "12345");
    Match match = new Match(MatchType.ID, 1.0f, Instant.now());

    IllegalStateException illegalStateException = new IllegalStateException("oops");
    BasicStream stream = createMovie("file://parent/test", attributes);

    when(idServiceForDS1.identify(eq(stream))).thenReturn(Map.of(stream.getId(), new Identification(identifier, match)));
    when(queryServiceForDS1.query(identifier)).thenThrow(illegalStateException);

    Exceptional<MediaIdentification> mi1 = Exceptional.from(() -> db.identify(stream, "D1"));  // id service available, returns result, but query throws exception
    MediaIdentification mi2 = db.identify(stream, "D2");  // id service available, but unable to identify
    Exceptional<MediaIdentification> mi5 = Exceptional.from(() -> db.identify(stream, "D5"));  // no id service at all

    assertEquals(Exceptional.ofException(illegalStateException), mi1);
    assertEquals(stream, mi2.getStream());
    assertEquals(Map.of(), mi2.getIdentifications());
    assertNull(mi2.getDescriptor());
    assertEquals(Exceptional.ofException(new UnknownDataSourceException(DATA_SOURCE_5)), mi5);
  }

  private static BasicStream createMovie(String uri, Attributes attributes) {
    return new BasicStream(MediaType.of("MOVIE"), new StringURI(uri), STREAM_ID, attributes, Collections.emptyList());
  }
}
