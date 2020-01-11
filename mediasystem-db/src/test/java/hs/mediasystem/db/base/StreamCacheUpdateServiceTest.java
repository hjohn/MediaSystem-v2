package hs.mediasystem.db.base;

import hs.mediasystem.db.base.DatabaseDescriptorStore;
import hs.mediasystem.db.base.DatabaseStreamStore;
import hs.mediasystem.db.base.StreamCacheUpdateService;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Identification;
import hs.mediasystem.domain.work.Identification.MatchType;
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
import hs.mediasystem.util.Tuple;

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
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class StreamCacheUpdateServiceTest {
  @Mock private DatabaseStreamStore streamStore;
  @Mock private DatabaseDescriptorStore descriptorStore;
  @Mock private LocalMediaIdentificationService identificationService;
  @InjectMocks private StreamCacheUpdateService updater;

  private final List<String> allowedDataSources = List.of("TMDB");

  private static final DataSource MOVIE_DATASOURCE = DataSource.instance(MediaType.of("MOVIE") ,"TMDB");
  private static final Identification IDENTIFICATION = new Identification(MatchType.NAME, 1.0, Instant.now());

  @BeforeEach
  public void before() {
    MockitoAnnotations.initMocks(this);

    when(streamStore.findStreamSource(any(StreamID.class))).thenReturn(new StreamSource(new StreamTags(Set.of("A")), allowedDataSources));
  }

  @Test
  public void shouldAddMedia() throws InterruptedException {
    BasicStream stream1 = basicStream(1234, "/home/user/Battlestar%20Galactica", "Battlestar Galactica");

    when(streamStore.findStream(new StreamID(1234))).thenReturn(Optional.of(stream1));
    when(identificationService.identify(stream1, allowedDataSources)).thenReturn(new MediaIdentification(stream1, Set.of(Exceptional.of(Tuple.of(
      new Identifier(MOVIE_DATASOURCE, "10000"), IDENTIFICATION, null
    )))));

    updater.update(1, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).put(eq(1), argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica")));
    verify(streamStore).putIdentifications(new StreamID(1234), Map.of(new Identifier(MOVIE_DATASOURCE, "10000"), IDENTIFICATION));
    verifyZeroInteractions(descriptorStore);
  }

  @Test
  public void shouldAddAndRemoveMedia() throws InterruptedException {
    when(streamStore.findByImportSourceId(1)).thenReturn(new HashMap<>(Map.of(
      new StreamID(20), basicStream(20, "/home/user/Battlestar%20Galactica", "Battlestar Galactica")
    )));

    BasicStream stream1 = basicStream(21, "/home/user/Battlestar%20Galactica%20Renamed", "Battlestar Galactica");
    Movie movie = Movies.create();

    when(identificationService.identify(stream1, allowedDataSources)).thenReturn(new MediaIdentification(stream1, Set.of(Exceptional.of(Tuple.of(
      new Identifier(MOVIE_DATASOURCE, "10000"), IDENTIFICATION, movie
    )))));

    when(streamStore.findStream(new StreamID(21))).thenReturn(Optional.of(stream1));

    updater.update(1, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).put(eq(1), argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica%20Renamed")));
    verify(streamStore).remove(new StreamID(20));
    verify(streamStore).putIdentifications(new StreamID(21), Map.of(new Identifier(MOVIE_DATASOURCE, "10000"), IDENTIFICATION));
    verify(descriptorStore).add(movie);
  }

  @Test
  public void shouldMergeExistingMediaWithoutDuplicateDataSources() throws InterruptedException {
    when(streamStore.findByImportSourceId(1)).thenReturn(new HashMap<>(Map.of(
      new StreamID(20), basicStream(20, "/home/user/Battlestar%20Galactica", "Battlestar Galactica")
    )));
    when(streamStore.findIdentifications(new StreamID(20))).thenReturn(Map.of(new Identifier(MOVIE_DATASOURCE, "10001"), IDENTIFICATION));

    BasicStream stream1 = basicStream(20, "/home/user/Battlestar%20Galactica%20Renamed", "Battlestar Galactica Renamed");
    Movie movie = Movies.create();

    // There already exists TMDB:10001 in store, now return TMDB:10000; only that one should be kept, as there should be no duplicate data sources in a record...
    when(identificationService.identify(stream1, allowedDataSources)).thenReturn(new MediaIdentification(stream1, Set.of(Exceptional.of(Tuple.of(
      new Identifier(MOVIE_DATASOURCE, "10000"), IDENTIFICATION, movie
    )))));

    when(streamStore.findStream(new StreamID(20))).thenReturn(Optional.of(stream1));  // Return renamed stream with same stream id (as content was same)

    updater.update(1, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).put(eq(1), argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica%20Renamed")));
//    verify(streamStore).remove(new StreamID(20));
    verify(streamStore).putIdentifications(new StreamID(20), Map.of(new Identifier(MOVIE_DATASOURCE, "10000"), IDENTIFICATION));
    verify(descriptorStore).add(movie);
  }

  @Test
  public void shouldReplaceExistingMediaIfAttributesDiffer() throws InterruptedException {
    when(streamStore.findByImportSourceId(1)).thenReturn(new HashMap<>(Map.of(
      new StreamID(123), basicStream(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica")
    )));

    BasicStream stream1 = basicStream(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica v2");

    when(identificationService.identify(stream1, allowedDataSources)).thenReturn(new MediaIdentification(stream1, Set.of(Exceptional.of(Tuple.of(
      new Identifier(MOVIE_DATASOURCE, "10000"), IDENTIFICATION, null
    )))));
    when(streamStore.findStream(new StreamID(123))).thenReturn(Optional.of(stream1));

    updater.update(1, List.of(Exceptional.of(List.of(
      stream1
    ))));

    Thread.sleep(100);  // Part of calls is async

    verify(streamStore).put(eq(1), argThat(ms -> ms.getAttributes().get(Attribute.TITLE).equals("Battlestar Galactica v2")));
    verify(streamStore).putIdentifications(new StreamID(123), Map.of(new Identifier(MOVIE_DATASOURCE, "10000"), IDENTIFICATION));
    verifyZeroInteractions(descriptorStore);
  }

  private static BasicStream basicStream(int identifier, String uri, String title, List<BasicStream> childStreams) {
    Attributes attributes = Attributes.of(Attribute.TITLE, title);

    return new BasicStream(MediaType.of("MOVIE"), new StringURI(uri), new StreamID(identifier), attributes, childStreams);
  }

  private static BasicStream basicStream(int identifier, String uri, String title) {
    return basicStream(identifier, uri, title, Collections.emptyList());
  }
}
