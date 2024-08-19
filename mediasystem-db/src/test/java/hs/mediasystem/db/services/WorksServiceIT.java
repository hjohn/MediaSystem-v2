package hs.mediasystem.db.services;

import hs.mediasystem.api.datasource.domain.Episode;
import hs.mediasystem.api.datasource.domain.Identification;
import hs.mediasystem.api.datasource.domain.Serie;
import hs.mediasystem.api.datasource.services.IdentificationProvider;
import hs.mediasystem.api.discovery.Attribute;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.db.Episodes;
import hs.mediasystem.db.InjectorExtension;
import hs.mediasystem.db.Movies;
import hs.mediasystem.db.Series;
import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.db.core.StreamTags;
import hs.mediasystem.db.core.Streamable;
import hs.mediasystem.db.core.StreamableEvent;
import hs.mediasystem.db.extract.StreamDescriptorService;
import hs.mediasystem.db.services.domain.ContentPrint;
import hs.mediasystem.db.services.domain.LinkedWork;
import hs.mediasystem.db.services.domain.Resource;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.events.SynchronousEventStream;
import hs.mediasystem.util.events.streams.EventStream;
import hs.mediasystem.util.image.ImageURI;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.int4.dirk.annotations.Produces;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(InjectorExtension.class)
public class WorksServiceIT {
  @Produces private static final StreamStateService STREAM_STATE_SERVICE = mock(StreamStateService.class);
  @Produces private static final EventStream<StreamableEvent> STREAMABLE_EVENTS = new SynchronousEventStream<>();
  @Produces private static final IdentificationStore IDENTIFICATION_STORE = mock(IdentificationStore.class);
  @Produces private static final StreamDescriptorService STREAM_DESCRIPTOR_SERVICE = mock(StreamDescriptorService.class);

  private static final DataSource INTERNAL = DataSource.instance("@INTERNAL");
  private static final DataSource EMDB = DataSource.instance("EMDB");
  private static final IdentificationProvider IDENTIFICATION_PROVIDER = mock(IdentificationProvider.class);
  private static final Instant DISCOVERY_TIME = Instant.now();

  @Inject ResourceService resourcesService;  // cannot be auto-discovered as not directly used, ensure it is present

  @Inject private LinkedWorksService linkedWorksService;


  @Nested
  class WhenEmpty {
    void findShouldNotFindAnything() {
      assertThat(linkedWorksService.find(new WorkId(INTERNAL, MediaType.MOVIE, "key"))).isEmpty();
    }

    void findAllByTypeShouldNotFindAnything() {
      assertThat(linkedWorksService.findAllByType(MediaType.MOVIE, null)).isEmpty();
    }
  }

  private static StreamableEvent.Updated streamableUpdated(MediaType mediaType, URI location, ContentPrint contentPrint) {
    return streamableUpdated(mediaType, location, contentPrint, null);
  }

  private static StreamableEvent.Updated streamableUpdated(MediaType mediaType, URI location, ContentPrint contentPrint, URI parentLocation) {
    return new StreamableEvent.Updated(
      new Streamable(
        mediaType,
        location,
        contentPrint,
        Optional.ofNullable(parentLocation),
        new StreamTags(Set.of("cartoons")),
        Optional.empty()
      ),
      Optional.of(IDENTIFICATION_PROVIDER),
      discovery(mediaType, location)
    );
  }

  private static Discovery discovery(MediaType mediaType, URI location) {
    return new Discovery(mediaType, location, Attributes.of(Attribute.TITLE, "Terminator"), DISCOVERY_TIME, 123L);
  }

  @Nested
  class WhenStreamsAreAdded {
    @BeforeEach
    void beforeEach() {
      STREAMABLE_EVENTS.push(streamableUpdated(
        MediaType.MOVIE,
        URI.create("file://Server/Movies/Terminator.avi"),
        new ContentPrint(new ContentID(2001), 12345L, DISCOVERY_TIME, new byte[16], DISCOVERY_TIME)
      ));
    }

    @AfterEach
    void afterEach() {
      STREAMABLE_EVENTS.push(new StreamableEvent.Removed(URI.create("file://Server/Movies/Terminator.avi")));
    }

    @Test
    void findShouldFindMovie() {
      await().untilAsserted(() -> {
        Optional<LinkedWork> optionalWork = linkedWorksService.find(new WorkId(INTERNAL, MediaType.MOVIE, "file://Server/Movies/Terminator.avi"));

        assertThat(optionalWork).isNotEmpty();

        LinkedWork linkedWork = optionalWork.orElseThrow();

        assertThat(linkedWork.workDescriptor().getDetails().getTitle()).isEqualTo("Terminator");
        assertThat(linkedWork.workDescriptor().getDetails().getCover()).isEmpty();
        assertThat(linkedWork.resources()).hasSize(1);

        Resource resource = linkedWork.resources().get(0);

        assertThat(resource.contentId()).isEqualTo(new ContentID(2001));
        assertThat(resource.match()).isEqualTo(new Match(Type.NONE, 1.0f, DISCOVERY_TIME));
      });
    }

    @Test
    void findAllByTypeShouldFindMovies() {
      await().untilAsserted(() -> {
        List<LinkedWork> works = linkedWorksService.findAllByType(MediaType.MOVIE, null);

        assertThat(works).hasSize(1);
      });
    }
  }

  @Nested
  class WhenIdentifiableStreamIsAdded {
    private final Instant matchTime = Instant.now();
    private final WorkId id = new WorkId(EMDB, MediaType.MOVIE, "T1000");

    @BeforeEach
    void beforeEach() throws SQLException {
      when(IDENTIFICATION_STORE.find(URI.create("file://Server/Movies/Terminator2.avi"))).thenReturn(Optional.of(new Identification(
        List.of(Movies.create(id, "The Terminator 2")),
        new Match(Type.NAME_AND_RELEASE_DATE, 0.99f, matchTime)
      )));

      STREAMABLE_EVENTS.push(streamableUpdated(
        MediaType.MOVIE,
        URI.create("file://Server/Movies/Terminator2.avi"),
        new ContentPrint(new ContentID(2002), 12345L, DISCOVERY_TIME, new byte[16], DISCOVERY_TIME)
      ));
    }

    @AfterEach
    void afterEach() {
      STREAMABLE_EVENTS.push(new StreamableEvent.Removed(URI.create("file://Server/Movies/Terminator2.avi")));
    }

    @Test
    void findShouldFindEnrichedMovie() {
      await().untilAsserted(() -> {
        Optional<LinkedWork> optionalWork = linkedWorksService.find(new WorkId(EMDB, MediaType.MOVIE, "T1000"));

        assertThat(optionalWork).isNotEmpty();

        LinkedWork linkedWork = optionalWork.orElseThrow();

        assertThat(linkedWork.workDescriptor().getDetails().getTitle()).isEqualTo("The Terminator 2");
        assertThat(linkedWork.workDescriptor().getDetails().getCover()).hasValue(new ImageURI("http://localhost", ""));
        assertThat(linkedWork.resources()).hasSize(1);

        Resource resource = linkedWork.resources().getFirst();

        assertThat(resource.contentId()).isEqualTo(new ContentID(2002));
        assertThat(resource.match()).isEqualTo(new Match(Type.NAME_AND_RELEASE_DATE, 0.99f, matchTime));
      });
    }

    @Test
    void findAllByTypeShouldFindMovies() {
      await().untilAsserted(() -> {
        List<LinkedWork> works = linkedWorksService.findAllByType(MediaType.MOVIE, null);

        assertThat(works).hasSize(1);
      });
    }
  }

  @Nested
  class WhenEpisodeStreamsAreAdded {
    private final Instant matchTime = Instant.now();
    private final WorkId id = new WorkId(EMDB, MediaType.SERIE, "SERIE-0001");
    private final WorkId episodeId1 = new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/01");
    private final WorkId episodeId2 = new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/02");
    private final WorkId episodeId3 = new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/03");
    private final Episode episode1 = Episodes.create(episodeId1, "eafp", 1, 1);
    private final Episode episode2 = Episodes.create(episodeId2, "tnn", 1, 2);
    private final Episode episode3 = Episodes.create(episodeId3, "coh", 1, 3);
    private final Serie serie = Series.create(id, "Stargate SG1", List.of(episode1, episode2, episode3));
    private final ContentPrint episode12CP = new ContentPrint(new ContentID(2011), 12345L, DISCOVERY_TIME, new byte[16], DISCOVERY_TIME);
    private final ContentPrint episode3CP = new ContentPrint(new ContentID(2012), 12345L, DISCOVERY_TIME, new byte[16], DISCOVERY_TIME);

    @BeforeEach
    void beforeEach() throws SQLException {

      /*
       * Scenario:
       * - Two streams exist, one claiming to be episode 1+2, and one claiming to be episode 3
       * - The external data source knows 3 episodes, but they don't match up with the streams (episode 1 is just an extended episode, and the stream for episode 3 is really episode 2).
       */

      when(IDENTIFICATION_PROVIDER.identifyChild(
        discovery(MediaType.EPISODE, URI.create("file://Server/Series/Stargate/Stargate.-.S01E01-02.avi")),
        serie
      )).thenReturn(new Identification(List.of(episode1, episode2), new Match(Type.DERIVED, 1.0f, matchTime)));

      when(IDENTIFICATION_PROVIDER.identifyChild(
        discovery(MediaType.EPISODE, URI.create("file://Server/Series/Stargate/Stargate.-.S01E03.avi")),
        serie
      )).thenReturn(new Identification(List.of(episode3), new Match(Type.DERIVED, 1.0f, matchTime)));

      when(IDENTIFICATION_STORE.find(URI.create("file://Server/Series/Stargate"))).thenReturn(Optional.of(new Identification(
        List.of(serie),
        new Match(Type.NAME, 1.0f, matchTime)
      )));

      STREAMABLE_EVENTS.push(streamableUpdated(
        MediaType.SERIE,
        URI.create("file://Server/Series/Stargate"),
        new ContentPrint(new ContentID(2010), 12345L, DISCOVERY_TIME, new byte[16], DISCOVERY_TIME)
      ));

      STREAMABLE_EVENTS.push(streamableUpdated(
        MediaType.EPISODE,
        URI.create("file://Server/Series/Stargate/Stargate.-.S01E01-02.avi"),
        episode12CP,
        URI.create("file://Server/Series/Stargate")
      ));

      STREAMABLE_EVENTS.push(streamableUpdated(
        MediaType.EPISODE,
        URI.create("file://Server/Series/Stargate/Stargate.-.S01E03.avi"),
        episode3CP,
        URI.create("file://Server/Series/Stargate")
      ));
    }

    @Test
    void findShouldFindIndividualEpisodes() {
      await().untilAsserted(() -> {
        LinkedWork work1 = linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/01")).orElseThrow();

        assertThat(work1.resources()).hasSize(1);

        LinkedWork work2 = linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/02")).orElseThrow();

        assertThat(work2.resources()).hasSize(1);

        LinkedWork work3 = linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/03")).orElseThrow();

        assertThat(work3.resources()).hasSize(1);

        assertThat(work1.resources()).isEqualTo(work2.resources());
        assertThat(work1.resources()).isNotEqualTo(work3.resources());
      });
    }

    @Test
    void findChildrenShouldFindChildrenOfSerie() {
      await().untilAsserted(() -> {
        List<LinkedWork> works = linkedWorksService.findChildren(new WorkId(EMDB, MediaType.SERIE, "SERIE-0001"));

        assertThat(works).hasSize(3);
      });
    }

    @Nested
    class AndThenRenamedToMatchExternalDataSource {
      @BeforeEach
      void beforeEach() {
        when(IDENTIFICATION_PROVIDER.identifyChild(
          discovery(MediaType.EPISODE, URI.create("file://Server/Series/Stargate/Stargate.-.S01E01.avi")),
          serie
        )).thenReturn(new Identification(List.of(episode1), new Match(Type.DERIVED, 1.0f, matchTime)));

        when(IDENTIFICATION_PROVIDER.identifyChild(
          discovery(MediaType.EPISODE, URI.create("file://Server/Series/Stargate/Stargate.-.S01E02.avi")),
          serie
        )).thenReturn(new Identification(List.of(episode2), new Match(Type.DERIVED, 1.0f, matchTime)));

        STREAMABLE_EVENTS.push(streamableUpdated(
          MediaType.EPISODE,
          URI.create("file://Server/Series/Stargate/Stargate.-.S01E01.avi"),
          episode12CP,  // content id stays the same, it was renamed
          URI.create("file://Server/Series/Stargate")
        ));

        STREAMABLE_EVENTS.push(streamableUpdated(
          MediaType.EPISODE,
          URI.create("file://Server/Series/Stargate/Stargate.-.S01E02.avi"),
          episode3CP,  // content id stays the same, it was renamed
          URI.create("file://Server/Series/Stargate")
        ));

        STREAMABLE_EVENTS.push(new StreamableEvent.Removed(URI.create("file://Server/Series/Stargate/Stargate.-.S01E01-02.avi")));
        STREAMABLE_EVENTS.push(new StreamableEvent.Removed(URI.create("file://Server/Series/Stargate/Stargate.-.S01E03.avi")));
      }

      @Test
      void findShouldFindIndividualEpisodes() {
        await().untilAsserted(() -> {
          LinkedWork work1 = linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/01")).orElseThrow();

          assertThat(work1.resources()).hasSize(1);

          LinkedWork work2 = linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/02")).orElseThrow();

          assertThat(work2.resources()).hasSize(1);

          assertThat(linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/03"))).isEmpty();
        });
      }

      @Test
      void findChildrenShouldFindChildrenOfSerie() {
        await().untilAsserted(() -> {
          List<LinkedWork> works = linkedWorksService.findChildren(new WorkId(EMDB, MediaType.SERIE, "SERIE-0001"));

          assertThat(works).hasSize(2);
        });
      }
    }
  }
}
