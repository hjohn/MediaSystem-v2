package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.MatchType;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.BasicStream;
import hs.mediasystem.mediamanager.LocalMediaIdentificationService;
import hs.mediasystem.mediamanager.MediaIdentification;
import hs.mediasystem.mediamanager.Movies;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.mediamanager.StreamTags;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.StringURI;

import java.time.Instant;
import java.util.Collections;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StreamCacheUpdateServiceTest {
  @Mock private DatabaseStreamStore streamStore;
  @Mock private DatabaseDescriptorStore descriptorStore;
  @Mock private LocalMediaIdentificationService identificationService;
  @InjectMocks private StreamCacheUpdateService updater;

  private final String allowedDataSource = "TMDB";

  private static final DataSource MOVIE_DATASOURCE = DataSource.instance(MediaType.of("MOVIE") ,"TMDB");
  private static final Match MATCH = new Match(MatchType.NAME, 1.0f, Instant.now());
  private static final Movie MOVIE = Movies.create();

  @BeforeEach
  public void before() {
    MockitoAnnotations.initMocks(this);

    when(streamStore.findStreamSource(any(StreamID.class))).thenReturn(new StreamSource(new StreamTags(Set.of("A")), List.of(allowedDataSource)));
  }

  @Test
  void shouldAddMedia() throws InterruptedException {
    BasicStream stream1 = basicStream(1234, "/home/user/Battlestar%20Galactica", "Battlestar Galactica");

    when(streamStore.findStream(new StreamID(1234))).thenReturn(Optional.of(stream1));
    when(identificationService.identify(stream1, allowedDataSource)).thenReturn(new MediaIdentification(
      stream1,
      Map.of(stream1.getId(), new Identification(new Identifier(MOVIE_DATASOURCE, "10000"), MATCH)),
      MOVIE
    ));

    updater.update(1, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).put(eq(1), argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica")));
    verify(streamStore).putIdentification(new StreamID(1234), new Identification(new Identifier(MOVIE_DATASOURCE, "10000"), MATCH));
    verify(descriptorStore).add(MOVIE);
  }

  @Test
  void shouldAddAndRemoveMedia() throws InterruptedException {
    when(streamStore.findByImportSourceId(1)).thenReturn(new HashMap<>(Map.of(
      new StreamID(20), basicStream(20, "/home/user/Battlestar%20Galactica", "Battlestar Galactica")
    )));

    BasicStream stream1 = basicStream(21, "/home/user/Battlestar%20Galactica%20Renamed", "Battlestar Galactica");

    when(identificationService.identify(stream1, allowedDataSource)).thenReturn(new MediaIdentification(
      stream1,
      Map.of(stream1.getId(), new Identification(new Identifier(MOVIE_DATASOURCE, "10000"), MATCH)),
      MOVIE
    ));

    when(streamStore.findStream(new StreamID(21))).thenReturn(Optional.of(stream1));

    updater.update(1, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).put(eq(1), argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica%20Renamed")));
    verify(streamStore).remove(new StreamID(20));
    verify(streamStore).putIdentification(new StreamID(21), new Identification(new Identifier(MOVIE_DATASOURCE, "10000"), MATCH));
    verify(descriptorStore).add(MOVIE);
  }

  @Test
  void shouldMergeExistingMediaWithoutDuplicateDataSources() throws InterruptedException {
    when(streamStore.findByImportSourceId(1)).thenReturn(new HashMap<>(Map.of(
      new StreamID(20), basicStream(20, "/home/user/Battlestar%20Galactica", "Battlestar Galactica")
    )));
    when(streamStore.findIdentification(new StreamID(20))).thenReturn(Optional.of(new Identification(new Identifier(MOVIE_DATASOURCE, "10001"), MATCH)));

    BasicStream stream1 = basicStream(20, "/home/user/Battlestar%20Galactica%20Renamed", "Battlestar Galactica Renamed");

    // There already exists TMDB:10001 in store, now return TMDB:10000; only that one should be kept, as there should be no duplicate data sources in a record...
    when(identificationService.identify(stream1, allowedDataSource)).thenReturn(new MediaIdentification(
      stream1,
      Map.of(stream1.getId(), new Identification(new Identifier(MOVIE_DATASOURCE, "10000"), MATCH)),
      MOVIE
    ));

    when(streamStore.findStream(new StreamID(20))).thenReturn(Optional.of(stream1));  // Return renamed stream with same stream id (as content was same)

    updater.update(1, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).put(eq(1), argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica%20Renamed")));
//    verify(streamStore).remove(new StreamID(20));
    verify(streamStore).putIdentification(new StreamID(20), new Identification(new Identifier(MOVIE_DATASOURCE, "10000"), MATCH));
    verify(descriptorStore).add(MOVIE);
  }

  @Test
  public void shouldReplaceExistingMediaIfAttributesDiffer() throws InterruptedException {
    when(streamStore.findByImportSourceId(1)).thenReturn(new HashMap<>(Map.of(
      new StreamID(123), basicStream(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica")
    )));

    BasicStream stream1 = basicStream(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica v2");

    when(identificationService.identify(stream1, allowedDataSource)).thenReturn(new MediaIdentification(
      stream1,
      Map.of(stream1.getId(), new Identification(new Identifier(MOVIE_DATASOURCE, "10000"), MATCH)),
      MOVIE
    ));
    when(streamStore.findStream(new StreamID(123))).thenReturn(Optional.of(stream1));

    updater.update(1, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).put(eq(1), argThat(ms -> ms.getAttributes().get(Attribute.TITLE).equals("Battlestar Galactica v2")));
    verify(streamStore).putIdentification(new StreamID(123), new Identification(new Identifier(MOVIE_DATASOURCE, "10000"), MATCH));
    verify(descriptorStore).add(MOVIE);
  }

  private static BasicStream basicStream(int identifier, String uri, String title, List<BasicStream> childStreams) {
    Attributes attributes = Attributes.of(Attribute.TITLE, title);

    return new BasicStream(MediaType.of("MOVIE"), new StringURI(uri), new StreamID(identifier), attributes, childStreams);
  }

  private static BasicStream basicStream(int identifier, String uri, String title) {
    return basicStream(identifier, uri, title, Collections.emptyList());
  }
}
