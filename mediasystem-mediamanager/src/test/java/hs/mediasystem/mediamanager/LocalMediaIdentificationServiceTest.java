package hs.mediasystem.mediamanager;

import hs.ddif.core.Injector;
import hs.ddif.jsr330.Injectors;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.Identification;
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

  private static final DataSource DATA_SOURCE_1 = DataSource.instance("D1");
  private static final DataSource DATA_SOURCE_2 = DataSource.instance("D2");
  private static final DataSource DATA_SOURCE_3 = DataSource.instance("D3");
  private static final DataSource DATA_SOURCE_4 = DataSource.instance("D4");
  private static final DataSource DATA_SOURCE_5 = DataSource.instance("D5");
  private static final DataSource DATA_SOURCE_6 = DataSource.instance("D6");

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
  public void before() {
    when(idServiceForDS1.getDataSource()).thenReturn(DATA_SOURCE_1);
    when(idServiceForDS1.getMediaType()).thenReturn(MediaType.MOVIE);
    when(idServiceForDS2.getDataSource()).thenReturn(DATA_SOURCE_2);
    when(idServiceForDS2.getMediaType()).thenReturn(MediaType.MOVIE);
    when(idServiceForDS3.getDataSource()).thenReturn(DATA_SOURCE_3);
    when(idServiceForDS3.getMediaType()).thenReturn(MediaType.EPISODE);
    when(idServiceForDS4.getDataSource()).thenReturn(DATA_SOURCE_4);
    when(idServiceForDS4.getMediaType()).thenReturn(MediaType.EPISODE);
    when(idServiceForDS6.getDataSource()).thenReturn(DATA_SOURCE_6);
    when(idServiceForDS6.getMediaType()).thenReturn(MediaType.MOVIE);
    when(serieIdService.getDataSource()).thenReturn(DataSource.instance("TMDB"));
    when(serieIdService.getMediaType()).thenReturn(MediaType.SERIE);
    when(episodeIdService.getDataSource()).thenReturn(DataSource.instance("TMDB"));
    when(episodeIdService.getMediaType()).thenReturn(MediaType.EPISODE);

    when(queryServiceForDS1.getDataSource()).thenReturn(DATA_SOURCE_1);
    when(queryServiceForDS1.getMediaType()).thenReturn(MediaType.MOVIE);
    when(queryServiceForDS3.getDataSource()).thenReturn(DATA_SOURCE_3);
    when(queryServiceForDS3.getMediaType()).thenReturn(MediaType.EPISODE);
    when(queryServiceForDS6.getDataSource()).thenReturn(DATA_SOURCE_6);
    when(queryServiceForDS6.getMediaType()).thenReturn(MediaType.MOVIE);

    Injector injector = Injectors.autoDiscovering();

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
    WorkId id1 = new WorkId(DATA_SOURCE_1, MediaType.MOVIE, "12345");
    WorkId id2 = new WorkId(DATA_SOURCE_2, MediaType.MOVIE, "12345");
    Match match = new Match(Type.ID, 1.0f, Instant.now());
    Streamable streamable = createMovie("file://parent/test", attributes);

    when(idServiceForDS1.identify(eq(streamable), eq(null))).thenReturn(Optional.of(new Identification(List.of(id1), match)));
    when(idServiceForDS2.identify(eq(streamable), eq(null))).thenReturn(Optional.of(new Identification(List.of(id2), match)));
    when(queryServiceForDS1.query(eq(id1))).thenReturn(Movies.create());

    MediaIdentification mi1 = db.identify(streamable, null, "D1");  // id service available and query works
    Exceptional<MediaIdentification> mi2 = Exceptional.from(() -> db.identify(streamable, null, "D2"));  // id service available and returns result, but query fails
    Exceptional<MediaIdentification> mi5 = Exceptional.from(() -> db.identify(streamable, null, "D5"));  // no id service at all

    assertEquals(streamable, mi1.getStreamable());
    assertEquals(new Identification(List.of(id1), match), mi1.getIdentification());
    assertNotNull(mi1.getDescriptor());
    assertEquals(Exceptional.ofException(new UnknownDataSourceException(new TypedDataSource(DATA_SOURCE_2, MediaType.MOVIE))), mi2);
    assertEquals(Exceptional.ofException(new UnknownDataSourceException(new TypedDataSource(DATA_SOURCE_5, MediaType.MOVIE))), mi5);
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
    assertEquals(Exceptional.ofException(new UnknownDataSourceException(new TypedDataSource(DATA_SOURCE_5, MediaType.MOVIE))), mi5);
  }

  @Test
  public void reindentifyShouldHandleQueryException() throws IOException {
    Attributes attributes = Attributes.of(Attribute.TITLE, "Title");
    WorkId id = new WorkId(DATA_SOURCE_1, MediaType.MOVIE, "12345");
    Match match = new Match(Type.ID, 1.0f, Instant.now());

    IllegalStateException illegalStateException = new IllegalStateException("oops");
    Streamable streamable = createMovie("file://parent/test", attributes);

    when(idServiceForDS1.identify(eq(streamable), eq(null))).thenReturn(Optional.of(new Identification(List.of(id), match)));
    when(idServiceForDS2.identify(eq(streamable), eq(null))).thenReturn(Optional.empty());
    when(queryServiceForDS1.query(id)).thenThrow(illegalStateException);

    Exceptional<MediaIdentification> mi1 = Exceptional.from(() -> db.identify(streamable, null, "D1"));  // id service available, returns result, but query throws exception
    Exceptional<MediaIdentification> mi2 = Exceptional.from(() -> db.identify(streamable, null, "D2"));  // id service available, but unable to identify
    Exceptional<MediaIdentification> mi5 = Exceptional.from(() -> db.identify(streamable, null, "D5"));  // no id service at all

    assertEquals(Exceptional.ofException(illegalStateException), mi1);
    assertEquals(Exceptional.ofException(new UnknownStreamableException(streamable, idServiceForDS2)), mi2);
    assertEquals(Exceptional.ofException(new UnknownDataSourceException(new TypedDataSource(DATA_SOURCE_5, MediaType.MOVIE))), mi5);
  }

  private static Streamable createMovie(String uri, Attributes attributes) {
    return new Streamable(MediaType.MOVIE, URI.create(uri), STREAM_ID, null, attributes);
  }
}
