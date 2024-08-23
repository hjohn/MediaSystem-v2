package hs.mediasystem.db.core;

import hs.mediasystem.api.datasource.domain.Episode;
import hs.mediasystem.api.datasource.domain.Identification;
import hs.mediasystem.api.datasource.domain.Serie;
import hs.mediasystem.api.datasource.services.IdentificationProvider;
import hs.mediasystem.api.discovery.Attribute;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.db.base.DatabaseContentPrintProvider;
import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.db.core.domain.ContentPrint;
import hs.mediasystem.db.core.domain.LinkedWork;
import hs.mediasystem.db.core.domain.Resource;
import hs.mediasystem.db.core.domain.StreamTags;
import hs.mediasystem.db.extract.StreamDescriptorService;
import hs.mediasystem.db.util.Episodes;
import hs.mediasystem.db.util.InjectorExtension;
import hs.mediasystem.db.util.Movies;
import hs.mediasystem.db.util.Series;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.image.ImageURI;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.int4.dirk.annotations.Produces;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(InjectorExtension.class)
public class WorksServiceIT {
  private static final ImportSource TMDB_IMPORT_SOURCE = new ImportSource(
    (root, registry) -> {},
    URI.create("/"),
    Optional.of("TMDB"),
    new StreamTags(Set.of())
  );

  @Produces private static final StreamStateService STREAM_STATE_SERVICE = mock(StreamStateService.class);
  @Produces private static final IdentificationStore IDENTIFICATION_STORE = mock(IdentificationStore.class);
  @Produces private static final StreamDescriptorService STREAM_DESCRIPTOR_SERVICE = mock(StreamDescriptorService.class);
  @Produces private static final DatabaseContentPrintProvider CONTENT_PRINT_PROVIDER = mock(DatabaseContentPrintProvider.class);
  @Produces private static final Collection<ImportSource> IMPORT_SOURCES = List.of(TMDB_IMPORT_SOURCE);
  @Produces private static final IdentificationProvider IDENTIFICATION_PROVIDER = spy(new IdentificationProvider() {

    @Override
    public String getName() {
      return "TMDB";
    }

    @Override
    public Optional<Identification> identify(Discovery discovery) {
      return Optional.empty();
    }

    @Override
    public Identification identifyChild(Discovery discovery, Identification parent) {
      return null;
    }
  });

  private static final DataSource INTERNAL = DataSource.instance("@INTERNAL");
  private static final DataSource TMDB = DataSource.instance("TMDB");
  private static final Instant DISCOVERY_TIME = Instant.now();

  @Inject private DiscoveryController discoveryController;
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

  private static Discovery discovery(MediaType mediaType, URI location) {
    return new Discovery(mediaType, location, Attributes.of(Attribute.TITLE, "Terminator"), DISCOVERY_TIME, 123L);
  }

  @Nested
  class WhenStreamsAreAdded {
    @BeforeEach
    void beforeEach() throws IOException {
      when(CONTENT_PRINT_PROVIDER.get(URI.create("/Terminator.avi"), 123L, DISCOVERY_TIME))
        .thenReturn(new ContentPrint(new ContentID(2001), 12345L, DISCOVERY_TIME, new byte[16], DISCOVERY_TIME));

      discoveryController.registerDiscovery(
        TMDB_IMPORT_SOURCE,
        URI.create("/"),
        List.of(
          discovery(MediaType.MOVIE, URI.create("/Terminator.avi"))
        )
      );
    }

    @Test
    void findShouldFindMovie() {
      await().untilAsserted(() -> {
        Optional<LinkedWork> optionalWork = linkedWorksService.find(new WorkId(INTERNAL, MediaType.MOVIE, "/Terminator.avi"));

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
    private final WorkId id = new WorkId(TMDB, MediaType.MOVIE, "T1000");

    @BeforeEach
    void beforeEach() throws SQLException, IOException {
      when(CONTENT_PRINT_PROVIDER.get(URI.create("/Terminator2.avi"), 123L, DISCOVERY_TIME))
        .thenReturn(new ContentPrint(new ContentID(2002), 12345L, DISCOVERY_TIME, new byte[16], DISCOVERY_TIME));

      when(IDENTIFICATION_STORE.find(URI.create("/Terminator2.avi"))).thenReturn(Optional.of(new Identification(
        List.of(Movies.create(id, "The Terminator 2")),
        new Match(Type.NAME_AND_RELEASE_DATE, 0.99f, matchTime)
      )));

      discoveryController.registerDiscovery(
        TMDB_IMPORT_SOURCE,
        URI.create("/"),
        List.of(
          discovery(MediaType.MOVIE, URI.create("/Terminator2.avi"))
        )
      );
    }

    @Test
    void findShouldFindEnrichedMovie() {
      await().untilAsserted(() -> {
        Optional<LinkedWork> optionalWork = linkedWorksService.find(new WorkId(TMDB, MediaType.MOVIE, "T1000"));

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
    private final WorkId id = new WorkId(TMDB, MediaType.SERIE, "SERIE-0001");
    private final WorkId episodeId1 = new WorkId(TMDB, MediaType.EPISODE, "SERIE-0001/01");
    private final WorkId episodeId2 = new WorkId(TMDB, MediaType.EPISODE, "SERIE-0001/02");
    private final WorkId episodeId3 = new WorkId(TMDB, MediaType.EPISODE, "SERIE-0001/03");
    private final Episode episode1 = Episodes.create(episodeId1, "eafp", 1, 1);
    private final Episode episode2 = Episodes.create(episodeId2, "tnn", 1, 2);
    private final Episode episode3 = Episodes.create(episodeId3, "coh", 1, 3);
    private final Serie serie = Series.create(id, "Stargate SG1", List.of(episode1, episode2, episode3));
    private final ContentPrint serieCP = new ContentPrint(new ContentID(2010), null, DISCOVERY_TIME, new byte[16], DISCOVERY_TIME);
    private final ContentPrint episode12CP = new ContentPrint(new ContentID(2011), 12345L, DISCOVERY_TIME, new byte[16], DISCOVERY_TIME);
    private final ContentPrint episode3CP = new ContentPrint(new ContentID(2012), 12345L, DISCOVERY_TIME, new byte[16], DISCOVERY_TIME);
    private final Identification identification = new Identification(List.of(serie), new Match(Type.NAME, 1.0f, matchTime));

    @BeforeEach
    void beforeEach() throws SQLException, IOException {

      /*
       * Scenario:
       * - Two streams exist, one claiming to be episode 1+2, and one claiming to be episode 3
       * - The external data source knows 3 episodes, but they don't match up with the streams (episode 1 is just an extended episode, and the stream for episode 3 is really episode 2).
       */

      when(CONTENT_PRINT_PROVIDER.get(URI.create("/Stargate"), 123L, DISCOVERY_TIME))
        .thenReturn(serieCP);

      when(CONTENT_PRINT_PROVIDER.get(URI.create("/Stargate/Stargate.-.S01E01-02.avi"), 123L, DISCOVERY_TIME))
        .thenReturn(episode12CP);

      when(CONTENT_PRINT_PROVIDER.get(URI.create("/Stargate/Stargate.-.S01E03.avi"), 123L, DISCOVERY_TIME))
        .thenReturn(episode3CP);

      when(IDENTIFICATION_PROVIDER.identifyChild(
        discovery(MediaType.EPISODE, URI.create("/Stargate/Stargate.-.S01E01-02.avi")),
        identification
      )).thenReturn(new Identification(List.of(episode1, episode2), new Match(Type.DERIVED, 1.0f, matchTime)));

      when(IDENTIFICATION_PROVIDER.identifyChild(
        discovery(MediaType.EPISODE, URI.create("/Stargate/Stargate.-.S01E03.avi")),
        identification
      )).thenReturn(new Identification(List.of(episode3), new Match(Type.DERIVED, 1.0f, matchTime)));

      when(IDENTIFICATION_PROVIDER.identify(
        discovery(MediaType.SERIE, URI.create("/Stargate"))
      )).thenReturn(Optional.of(identification));

      when(IDENTIFICATION_STORE.find(URI.create("/Stargate"))).thenReturn(Optional.of(identification));

      discoveryController.registerDiscovery(
        TMDB_IMPORT_SOURCE,
        URI.create("/"),
        List.of(
          discovery(MediaType.SERIE, URI.create("/Stargate"))
        )
      );

      discoveryController.registerDiscovery(
        TMDB_IMPORT_SOURCE,
        URI.create("/Stargate"),
        List.of(
          discovery(MediaType.EPISODE, URI.create("/Stargate/Stargate.-.S01E01-02.avi")),
          discovery(MediaType.EPISODE, URI.create("/Stargate/Stargate.-.S01E03.avi"))
        )
      );
    }

    @Test
    void findShouldFindIndividualEpisodes() {
      await().untilAsserted(() -> {
        LinkedWork work1 = linkedWorksService.find(new WorkId(TMDB, MediaType.EPISODE, "SERIE-0001/01")).orElseThrow(() -> new AssertionError());

        assertThat(work1.resources()).hasSize(1);

        LinkedWork work2 = linkedWorksService.find(new WorkId(TMDB, MediaType.EPISODE, "SERIE-0001/02")).orElseThrow(() -> new AssertionError());

        assertThat(work2.resources()).hasSize(1);

        LinkedWork work3 = linkedWorksService.find(new WorkId(TMDB, MediaType.EPISODE, "SERIE-0001/03")).orElseThrow(() -> new AssertionError());

        assertThat(work3.resources()).hasSize(1);

        assertThat(work1.resources()).isEqualTo(work2.resources());
        assertThat(work1.resources()).isNotEqualTo(work3.resources());
      });
    }

    @Test
    void findChildrenShouldFindChildrenOfSerie() {
      await().untilAsserted(() -> {
        List<LinkedWork> works = linkedWorksService.findChildren(new WorkId(TMDB, MediaType.SERIE, "SERIE-0001"));

        assertThat(works).hasSize(3);
      });
    }

    @Nested
    class AndThenRenamedToMatchExternalDataSource {
      @BeforeEach
      void beforeEach() throws IOException {
        when(CONTENT_PRINT_PROVIDER.get(URI.create("/Stargate/Stargate.-.S01E01.avi"), 123L, DISCOVERY_TIME))
          .thenReturn(episode12CP);  // content id will be the same as it is just a rename

        when(CONTENT_PRINT_PROVIDER.get(URI.create("/Stargate/Stargate.-.S01E02.avi"), 123L, DISCOVERY_TIME))
          .thenReturn(episode3CP);  // content id will be the same as it is just a rename

        when(IDENTIFICATION_PROVIDER.identifyChild(
          discovery(MediaType.EPISODE, URI.create("/Stargate/Stargate.-.S01E01.avi")),
          identification
        )).thenReturn(new Identification(List.of(episode1), new Match(Type.DERIVED, 1.0f, matchTime)));

        when(IDENTIFICATION_PROVIDER.identifyChild(
          discovery(MediaType.EPISODE, URI.create("/Stargate/Stargate.-.S01E02.avi")),
          identification
        )).thenReturn(new Identification(List.of(episode2), new Match(Type.DERIVED, 1.0f, matchTime)));

        discoveryController.registerDiscovery(
          TMDB_IMPORT_SOURCE,
          URI.create("/Stargate"),
          List.of(
            discovery(MediaType.EPISODE, URI.create("/Stargate/Stargate.-.S01E01.avi")),
            discovery(MediaType.EPISODE, URI.create("/Stargate/Stargate.-.S01E02.avi"))
          )
        );
      }

      @Test
      void findShouldFindIndividualEpisodes() {
        await().untilAsserted(() -> {
          LinkedWork work1 = linkedWorksService.find(new WorkId(TMDB, MediaType.EPISODE, "SERIE-0001/01")).orElseThrow(() -> new AssertionError());

          assertThat(work1.resources()).hasSize(1);

          LinkedWork work2 = linkedWorksService.find(new WorkId(TMDB, MediaType.EPISODE, "SERIE-0001/02")).orElseThrow(() -> new AssertionError());

          assertThat(work2.resources()).hasSize(1);

          assertThat(linkedWorksService.find(new WorkId(TMDB, MediaType.EPISODE, "SERIE-0001/03"))).isEmpty();
        });
      }

      @Test
      void findChildrenShouldFindChildrenOfSerie() {
        await().untilAsserted(() -> {
          List<LinkedWork> works = linkedWorksService.findChildren(new WorkId(TMDB, MediaType.SERIE, "SERIE-0001"));

          assertThat(works).hasSize(2);
        });
      }
    }
  }
}
