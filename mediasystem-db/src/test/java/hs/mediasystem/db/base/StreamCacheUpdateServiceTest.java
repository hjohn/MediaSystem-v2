package hs.mediasystem.db.base;

import hs.mediasystem.db.DatabaseResponseCache;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.mediamanager.LocalMediaIdentificationService;
import hs.mediasystem.mediamanager.MediaIdentification;
import hs.mediasystem.mediamanager.Movies;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.mediamanager.StreamTags;
import hs.mediasystem.util.Attributes;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StreamCacheUpdateServiceTest {
  @Mock private DatabaseStreamStore streamStore;
  @Mock private DatabaseDescriptorStore descriptorStore;
  @Mock private LocalMediaIdentificationService identificationService;
  @Mock private DatabaseResponseCache cache;
  @Mock private StreamCacheUpdater updater;
  @InjectMocks private StreamCacheUpdateService service;

  private final String allowedDataSource = "TMDB";
  private final Map<StreamID, MediaIdentification> knownIds = new HashMap<>();

  private static final DataSource TMDB = DataSource.instance("TMDB");
  private static final Match MATCH = new Match(Type.NAME, 1.0f, Instant.now());
  private static final Movie MOVIE = Movies.create();

  @BeforeEach
  public void before() {
    when(streamStore.findStreamSource(any(StreamID.class))).thenReturn(new StreamSource(new StreamTags(Set.of("A")), List.of(allowedDataSource)));
    when(updater.asyncEnrich(any(), any(), any())).then(i -> {
      BiFunction<StreamID, MediaIdentification, MediaIdentification> task = i.getArgument(2);
      Streamable streamable = i.getArgument(1);
      MediaIdentification mi = streamable.getParentId().map(knownIds::get).orElse(null);

      return CompletableFuture.completedFuture(task.apply(streamable.getId(), mi));
    });
  }

  @Test
  void shouldAddMedia() throws IOException {
    Streamable stream1 = streamable(1234, "/home/user/Battlestar%20Galactica", "Battlestar Galactica");
    StreamID streamId1 = stream1.getId();

    when(streamStore.findStream(streamId1)).thenReturn(Optional.of(stream1));
    when(identificationService.identify(stream1, null, allowedDataSource)).thenReturn(new MediaIdentification(
      stream1,
      new Identification(List.of(new WorkId(TMDB, MediaType.MOVIE, "10000")), MATCH),
      MOVIE
    ));

    service.update(7, List.of(stream1));

    verify(streamStore).put(argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica")));
    verify(streamStore).putIdentification(streamId1, new Identification(List.of(new WorkId(TMDB, MediaType.MOVIE, "10000")), MATCH));
    verify(descriptorStore).add(MOVIE);
  }

  @Test
  void shouldAddAndRemoveMedia() throws IOException {
    Streamable streamable1 = streamable(20, "/home/user/Battlestar%20Galactica", "Battlestar Galactica");
    Streamable streamable2 = streamable(21, "/home/user/Battlestar%20Galactica%20Renamed", "Battlestar Galactica");

    when(streamStore.findByImportSourceId(7)).thenReturn(new HashMap<>(Map.of(
      streamable1.getId(), streamable1
    )));

    when(identificationService.identify(streamable2, null, allowedDataSource)).thenReturn(new MediaIdentification(
      streamable2,
      new Identification(List.of(new WorkId(TMDB, MediaType.MOVIE, "10000")), MATCH),
      MOVIE
    ));

    when(streamStore.findStream(streamable2.getId())).thenReturn(Optional.of(streamable2));

    service.update(7, List.of(streamable2));

    verify(streamStore).put(argThat(s -> s.getUri().toString().equals("/home/user/Battlestar%20Galactica%20Renamed")));
    verify(streamStore).remove(streamable1.getId());
    verify(streamStore).putIdentification(streamable2.getId(), new Identification(List.of(new WorkId(TMDB, MediaType.MOVIE, "10000")), MATCH));
    verify(descriptorStore).add(MOVIE);
  }

  @Test
  public void shouldReplaceExistingMediaIfAttributesDiffer() throws IOException {
    Streamable streamable1 = streamable(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica");
    Streamable streamable2 = streamable(123, "/home/user/Battlestar%20Galactica", "Battlestar Galactica v2");

    when(streamStore.findByImportSourceId(7)).thenReturn(new HashMap<>(Map.of(streamable1.getId(), streamable1)));
    when(streamStore.findStream(streamable2.getId())).thenReturn(Optional.of(streamable2));

    when(identificationService.identify(streamable2, null, allowedDataSource)).thenReturn(new MediaIdentification(
      streamable2,
      new Identification(List.of(new WorkId(TMDB, MediaType.MOVIE, "10000")), MATCH),
      MOVIE
    ));

    service.update(7, List.of(streamable2));

    verify(streamStore).put(argThat(ms -> ms.getAttributes().get(Attribute.TITLE).equals("Battlestar Galactica v2")));
    verify(streamStore).putIdentification(streamable2.getId(), new Identification(List.of(new WorkId(TMDB, MediaType.MOVIE, "10000")), MATCH));
    verify(descriptorStore).add(MOVIE);
  }

  @Nested
  class WhenStoreIsEmpty {
    @Nested
    class AndASerieIsDiscovered {
      Streamable serie = streamable(123, "/home/user/Serie", "Serie");
      Identification identification = new Identification(List.of(new WorkId(TMDB, MediaType.MOVIE, "10000")), MATCH);
      MediaIdentification serieMediaIdentification = new MediaIdentification(serie, identification, MOVIE);

      @BeforeEach
      void beforeEach() throws IOException {
        when(streamStore.findByImportSourceId(7)).thenReturn(new HashMap<>());
        when(streamStore.findStream(serie.getId())).thenReturn(Optional.of(serie));
        when(identificationService.identify(serie, null, allowedDataSource)).thenReturn(serieMediaIdentification);

        service.update(7, List.of(serie));

        knownIds.put(serie.getId(), serieMediaIdentification);
      }

      @Test
      void shouldAddSerieAndMarkItEnriched() {
        verify(streamStore).put(serie);
        verify(streamStore).markEnriched(serie.getId());
        verify(streamStore).putIdentification(serie.getId(), identification);
        verifyNoMoreInteractions(streamStore);
      }

      @Nested
      class AndAnEpisodeIsDiscovered {
        Streamable episode1 = childStreamable(serie.getId(), 124, "/home/user/Serie/Episode1", "Episode1");
        Streamable episode2 = childStreamable(serie.getId(), 125, "/home/user/Serie/Episode2", "Episode2");
        Identification episode1Identification = new Identification(List.of(new WorkId(TMDB, MediaType.MOVIE, "10000:ep1")), MATCH);
        Identification episode2Identification = new Identification(List.of(new WorkId(TMDB, MediaType.MOVIE, "10000:ep2")), MATCH);
        MediaIdentification episode1MediaIdentification = new MediaIdentification(episode1, episode1Identification, MOVIE);
        MediaIdentification episode2MediaIdentification = new MediaIdentification(episode1, episode2Identification, MOVIE);

        @BeforeEach
        void beforeEach() throws IOException {
          when(streamStore.findByImportSourceId(7)).thenReturn(new HashMap<>(Map.of(serie.getId(), serie)));
          when(streamStore.findStream(episode1.getId())).thenReturn(Optional.of(episode1));
          when(streamStore.findStream(episode2.getId())).thenReturn(Optional.of(episode2));
          when(identificationService.identify(episode1, serieMediaIdentification.getDescriptor(), allowedDataSource)).thenReturn(episode1MediaIdentification);
          when(identificationService.identify(episode2, serieMediaIdentification.getDescriptor(), allowedDataSource)).thenReturn(episode2MediaIdentification);

          service.update(7, List.of(serie, episode1, episode2));
        }

        @Test
        void shouldAddEpisodeAndMarkItEnriched() {
          verify(streamStore).put(serie);
          verify(streamStore).put(episode1);
          verify(streamStore).put(episode2);
          verify(streamStore).markEnriched(episode1.getId());
          verify(streamStore).markEnriched(episode2.getId());
          verify(streamStore).markEnriched(serie.getId());  // once only
        }
      }

      @Nested
      class AndAnUnknownEpisodeIsDiscovered {
        void shouldRefreshSerie() {
        }
      }

      @Nested
      class AndAnUnmatchableFileIsDiscovered {
        void shouldRefreshSerie() {
        }

        @Nested
        class AndTheExistingUnmatchableFileIsDiscoveredAgain {
          void shouldNotRefreshSerie() {
          }
        }
      }
    }
  }

  private static Streamable streamable(int identifier, String uri, String title) {
    Attributes attributes = Attributes.of(Attribute.TITLE, title);

    return new Streamable(MediaType.MOVIE, URI.create(uri), new StreamID(7, new ContentID(identifier), title), null, attributes);
  }

  private static Streamable childStreamable(StreamID parentStreamId, int identifier, String uri, String title) {
    Attributes attributes = Attributes.of(Attribute.TITLE, title);

    return new Streamable(MediaType.MOVIE, URI.create(uri), new StreamID(7, new ContentID(identifier), title), parentStreamId, attributes);
  }
}
