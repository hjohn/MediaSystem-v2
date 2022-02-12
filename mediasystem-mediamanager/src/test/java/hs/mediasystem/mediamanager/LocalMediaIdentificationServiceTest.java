package hs.mediasystem.mediamanager;

import hs.ddif.core.Injector;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Exceptional;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LocalMediaIdentificationServiceTest {
  private static final StreamID STREAM_ID = new StreamID(1, new ContentID(999), "Stuff");

  private static final DataSource DATA_SOURCE_1 = DataSource.instance(MediaType.MOVIE, "D1");
  private static final DataSource DATA_SOURCE_2 = DataSource.instance(MediaType.MOVIE, "D2");
  private static final DataSource DATA_SOURCE_3 = DataSource.instance(MediaType.EPISODE, "D3");
  private static final DataSource DATA_SOURCE_4 = DataSource.instance(MediaType.EPISODE, "D4");
  private static final DataSource DATA_SOURCE_5 = DataSource.instance(MediaType.MOVIE, "D5");
  private static final DataSource DATA_SOURCE_6 = DataSource.instance(MediaType.MOVIE, "D6");

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

  @BeforeEach
  public void before() throws Exception {
    when(idServiceForDS1.getDataSource()).thenReturn(DATA_SOURCE_1);
    when(idServiceForDS2.getDataSource()).thenReturn(DATA_SOURCE_2);
    when(idServiceForDS3.getDataSource()).thenReturn(DATA_SOURCE_3);
    when(idServiceForDS4.getDataSource()).thenReturn(DATA_SOURCE_4);
    when(idServiceForDS6.getDataSource()).thenReturn(DATA_SOURCE_6);
    when(serieIdService.getDataSource()).thenReturn(DataSource.instance(MediaType.SERIE, "TMDB"));
    when(episodeIdService.getDataSource()).thenReturn(DataSource.instance(MediaType.EPISODE, "TMDB"));

    when(queryServiceForDS1.getDataSource()).thenReturn(DATA_SOURCE_1);
    when(queryServiceForDS3.getDataSource()).thenReturn(DATA_SOURCE_3);
    when(queryServiceForDS6.getDataSource()).thenReturn(DATA_SOURCE_6);

    Injector injector = new Injector(true);

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
  public void reidentifyShouldCreateIdentificationWithoutMatchingQueryService() throws IOException {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    Identifier identifier1 = new Identifier(DATA_SOURCE_1, "12345");
    Identifier identifier2 = new Identifier(DATA_SOURCE_2, "12345");
    Match match = new Match(Type.ID, 1.0f, Instant.now());
    Streamable streamable = createMovie("file://parent/test", attributes);

    when(idServiceForDS1.identify(eq(streamable), eq(null))).thenReturn(Optional.of(new Identification(List.of(identifier1), match)));
    when(idServiceForDS2.identify(eq(streamable), eq(null))).thenReturn(Optional.of(new Identification(List.of(identifier2), match)));
    when(queryServiceForDS1.query(eq(identifier1))).thenReturn(Movies.create());

    MediaIdentification mi1 = db.identify(streamable, null, "D1");  // id service available and query works
    Exceptional<MediaIdentification> mi2 = Exceptional.from(() -> db.identify(streamable, null, "D2"));  // id service available and returns result, but query fails
    Exceptional<MediaIdentification> mi5 = Exceptional.from(() -> db.identify(streamable, null, "D5"));  // no id service at all

    assertEquals(streamable, mi1.getStreamable());
    assertEquals(new Identification(List.of(identifier1), match), mi1.getIdentification());
    assertNotNull(mi1.getDescriptor());
    assertEquals(Exceptional.ofException(new UnknownDataSourceException(DATA_SOURCE_2)), mi2);
    assertEquals(Exceptional.ofException(new UnknownDataSourceException(DATA_SOURCE_5)), mi5);
  }

  @Test
  public void reindentifyShouldHandleIdentifyException() throws IOException {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");

    IllegalStateException illegalStateException = new IllegalStateException("oops");
    Streamable streamable = createMovie("file://parent/test", attributes);

    when(idServiceForDS1.identify(eq(streamable), eq(null))).thenReturn(Optional.empty());
    when(idServiceForDS2.identify(eq(streamable), eq(null))).thenThrow(illegalStateException);

    Exceptional<MediaIdentification> mi1 = Exceptional.from(() -> db.identify(streamable, null, "D1"));  // id service available, but unable to identify
    Exceptional<MediaIdentification> mi2 = Exceptional.from(() -> db.identify(streamable, null, "D2"));  // id service available, but throws exception
    Exceptional<MediaIdentification> mi5 = Exceptional.from(() -> db.identify(streamable, null, "D5"));  // no id service at all

    assertEquals(Exceptional.ofException(new UnknownStreamableException(streamable, idServiceForDS1)), mi1);
    assertEquals(Exceptional.ofException(illegalStateException), mi2);
    assertEquals(Exceptional.ofException(new UnknownDataSourceException(DATA_SOURCE_5)), mi5);
  }

  @Test
  public void reindentifyShouldHandleQueryException() throws IOException {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    Identifier identifier = new Identifier(DATA_SOURCE_1, "12345");
    Match match = new Match(Type.ID, 1.0f, Instant.now());

    IllegalStateException illegalStateException = new IllegalStateException("oops");
    Streamable streamable = createMovie("file://parent/test", attributes);

    when(idServiceForDS1.identify(eq(streamable), eq(null))).thenReturn(Optional.of(new Identification(List.of(identifier), match)));
    when(idServiceForDS2.identify(eq(streamable), eq(null))).thenReturn(Optional.empty());
    when(queryServiceForDS1.query(identifier)).thenThrow(illegalStateException);

    Exceptional<MediaIdentification> mi1 = Exceptional.from(() -> db.identify(streamable, null, "D1"));  // id service available, returns result, but query throws exception
    Exceptional<MediaIdentification> mi2 = Exceptional.from(() -> db.identify(streamable, null, "D2"));  // id service available, but unable to identify
    Exceptional<MediaIdentification> mi5 = Exceptional.from(() -> db.identify(streamable, null, "D5"));  // no id service at all

    assertEquals(Exceptional.ofException(illegalStateException), mi1);
    assertEquals(Exceptional.ofException(new UnknownStreamableException(streamable, idServiceForDS2)), mi2);
    assertEquals(Exceptional.ofException(new UnknownDataSourceException(DATA_SOURCE_5)), mi5);
  }

  private static Streamable createMovie(String uri, Attributes attributes) {
    return new Streamable(MediaType.MOVIE, URI.create(uri), STREAM_ID, null, attributes);
  }
}
