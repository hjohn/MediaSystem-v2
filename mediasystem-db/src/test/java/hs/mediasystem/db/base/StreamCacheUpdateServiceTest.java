package hs.mediasystem.db.base;

import hs.mediasystem.db.DatabaseResponseCache;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.mediamanager.LocalMediaIdentificationService;
import hs.mediasystem.mediamanager.MediaIdentification;
import hs.mediasystem.mediamanager.Movies;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.mediamanager.StreamTags;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StreamCacheUpdateServiceTest {
  @Mock private DatabaseStreamStore streamStore;
  @Mock private DatabaseDescriptorStore descriptorStore;
  @Mock private LocalMediaIdentificationService identificationService;
  @Mock private DatabaseResponseCache cache;
  @InjectMocks private StreamCacheUpdateService updater;

  private final String allowedDataSource = "TMDB";

  private static final DataSource MOVIE_DATASOURCE = DataSource.instance(MediaType.of("MOVIE") ,"TMDB");
  private static final Match MATCH = new Match(Type.NAME, 1.0f, Instant.now());
  private static final Movie MOVIE = Movies.create();

  @BeforeEach
  public void before() {
    MockitoAnnotations.initMocks(this);

    when(streamStore.findStreamSource(any(StreamID.class))).thenReturn(new StreamSource(new StreamTags(Set.of("A")), List.of(allowedDataSource)));
  }

  @Test
  void shouldAddMedia() throws InterruptedException {
    Streamable stream1 = streamable(1234, "/home/user/Battlestar%20Galactica", "Battlestar Galactica");
    StreamID streamId1 = stream1.getId();

    when(streamStore.findStream(streamId1)).thenReturn(Optional.of(stream1));
    when(identificationService.identify(stream1, null, allowedDataSource)).thenReturn(new MediaIdentification(
      stream1,
      new Identification(List.of(new Identifier(MOVIE_DATASOURCE, "10000")), MATCH),
      MOVIE
    ));

    updater.update(7, List.of(stream1));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).put(argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica")));
    verify(streamStore).putIdentification(streamId1, new Identification(List.of(new Identifier(MOVIE_DATASOURCE, "10000")), MATCH));
    verify(descriptorStore).add(MOVIE);
  }

  @Test
  void shouldAddAndRemoveMedia() throws InterruptedException {
    Streamable streamable1 = streamable(20, "/home/user/Battlestar%20Galactica", "Battlestar Galactica");
    Streamable streamable2 = streamable(21, "/home/user/Battlestar%20Galactica%20Renamed", "Battlestar Galactica");

    when(streamStore.findByImportSourceId(7)).thenReturn(new HashMap<>(Map.of(
      streamable1.getId(), streamable1
    )));

    when(identificationService.identify(streamable2, null, allowedDataSource)).thenReturn(new MediaIdentification(
      streamable2,
      new Identification(List.of(new Identifier(MOVIE_DATASOURCE, "10000")), MATCH),
      MOVIE
    ));

    when(streamStore.findStream(streamable2.getId())).thenReturn(Optional.of(streamable2));

    updater.update(7, List.of(streamable2));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).put(argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica%20Renamed")));
    verify(streamStore).remove(streamable1.getId());
    verify(streamStore).putIdentification(streamable2.getId(), new Identification(List.of(new Identifier(MOVIE_DATASOURCE, "10000")), MATCH));
    verify(descriptorStore).add(MOVIE);
  }

  @Test
  public void shouldReplaceExistingMediaIfAttributesDiffer() throws InterruptedException {
    Streamable streamable1 = streamable(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica");
    Streamable streamable2 = streamable(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica v2");

    when(streamStore.findByImportSourceId(7)).thenReturn(new HashMap<>(Map.of(streamable1.getId(), streamable1)));
    when(streamStore.findStream(streamable2.getId())).thenReturn(Optional.of(streamable2));

    when(identificationService.identify(streamable2, null, allowedDataSource)).thenReturn(new MediaIdentification(
      streamable2,
      new Identification(List.of(new Identifier(MOVIE_DATASOURCE, "10000")), MATCH),
      MOVIE
    ));

    updater.update(7, List.of(streamable2));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).put(argThat(ms -> ms.getAttributes().get(Attribute.TITLE).equals("Battlestar Galactica v2")));
    verify(streamStore).putIdentification(streamable2.getId(), new Identification(List.of(new Identifier(MOVIE_DATASOURCE, "10000")), MATCH));
    verify(descriptorStore).add(MOVIE);
  }

  private static Streamable streamable(int identifier, String uri, String title) {
    Attributes attributes = Attributes.of(Attribute.TITLE, title);

    return new Streamable(MediaType.of("MOVIE"), new StringURI(uri), new StreamID(7, new ContentID(identifier), title), null, attributes);
  }
}
