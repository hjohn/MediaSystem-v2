package hs.mediasystem.db.services;

import hs.ddif.annotations.Produces;
import hs.mediasystem.db.InjectorExtension;
import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.db.core.IdentificationEvent;
import hs.mediasystem.db.core.ResourceEvent;
import hs.mediasystem.db.services.domain.LinkedWork;
import hs.mediasystem.db.services.domain.MatchedResource;
import hs.mediasystem.db.services.domain.Resource;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.api.StreamTags;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.mediamanager.Episodes;
import hs.mediasystem.mediamanager.Movies;
import hs.mediasystem.mediamanager.Series;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.events.InMemoryEventStore;
import hs.mediasystem.util.events.PersistentEventStream;
import hs.mediasystem.util.events.SynchronousEventStream;
import hs.mediasystem.util.events.streams.EventStream;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

@ExtendWith(InjectorExtension.class)
public class WorksServiceIT {
  @Produces private static final EventStream<ResourceEvent> RESOURCE_EVENTS = new SynchronousEventStream<>();
  @Produces private static final PersistentEventStream<IdentificationEvent> IDENTIFICATION_EVENTS = new PersistentEventStream<>(new InMemoryEventStore<>(IdentificationEvent.class));
  @Produces private static final StreamStateService STREAM_STATE_SERVICE = mock(StreamStateService.class);

  private static final DataSource INTERNAL = DataSource.instance("@INTERNAL");
  private static final DataSource EMDB = DataSource.instance("EMDB");

  @Inject LinkedResourcesService linkedResourcesService;  // cannot be auto-discovered as not directly used, ensure it is present

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

  @Nested
  class WhenStreamsAreAdded {
    private final Instant discoveryTime = Instant.now();

    {
      RESOURCE_EVENTS.push(new ResourceEvent.Updated(new Resource(
        Path.of("//Server/Movies/Terminator.avi").toUri(),
        Optional.empty(),
        MediaType.MOVIE,
        new ContentID(2001),
        Instant.now(),
        Optional.of(12345L),
        discoveryTime,
        new StreamTags(Set.of("cartoons")),
        Optional.empty(),
        Optional.empty(),
        List.of(),
        Attributes.of(Attribute.TITLE, "Terminator")
      )));
    }

    @Test
    void findShouldFindMovie() {
      await().untilAsserted(() -> {
        Optional<LinkedWork> optionalWork = linkedWorksService.find(new WorkId(INTERNAL, MediaType.MOVIE, "file://Server/Movies/Terminator.avi"));

        assertThat(optionalWork).isNotEmpty();

        LinkedWork linkedWork = optionalWork.orElseThrow();

        assertThat(linkedWork.work().descriptor().getDetails().getTitle()).isEqualTo("Terminator");
        assertThat(linkedWork.work().descriptor().getDetails().getCover()).isEmpty();
        assertThat(linkedWork.matchedResources()).hasSize(1);

        MatchedResource matchedResource = linkedWork.matchedResources().get(0);

        assertThat(matchedResource.resource().contentId()).isEqualTo(new ContentID(2001));
        assertThat(matchedResource.match()).isEqualTo(new Match(Type.NONE, 1.0f, matchedResource.resource().discoveryTime()));
      });
    }

    @Test
    void findAllByTypeShouldFindMovies() {
      await().untilAsserted(() -> {
        List<LinkedWork> works = linkedWorksService.findAllByType(MediaType.MOVIE, null);

        assertThat(works).hasSize(1);
      });
    }

    @Nested
    class AndThenEnriched {
      private final Instant matchTime = Instant.now();
      private final WorkId id = new WorkId(EMDB, MediaType.MOVIE, "T800");

      {
        IDENTIFICATION_EVENTS.push(new IdentificationEvent(
          Path.of("//Server/Movies/Terminator.avi").toUri(),
          List.of(Movies.create(id, "The Terminator")),
          new Match(Type.NAME_AND_RELEASE_DATE, 0.99f, matchTime)
        ));
      }

      @Test
      void findShouldFindEnrichedMovie() {
        await().untilAsserted(() -> {
          Optional<LinkedWork> optionalWork = linkedWorksService.find(new WorkId(EMDB, MediaType.MOVIE, "T800"));

          assertThat(optionalWork).isNotEmpty();

          LinkedWork linkedWork = optionalWork.orElseThrow();

          assertThat(linkedWork.work().descriptor().getDetails().getTitle()).isEqualTo("The Terminator");
          assertThat(linkedWork.work().descriptor().getDetails().getCover()).hasValue(new ImageURI("http://localhost", ""));
          assertThat(linkedWork.matchedResources()).hasSize(1);

          MatchedResource matchedResource = linkedWork.matchedResources().get(0);

          assertThat(matchedResource.resource().contentId()).isEqualTo(new ContentID(2001));
          assertThat(matchedResource.match()).isEqualTo(new Match(Type.NAME_AND_RELEASE_DATE, 0.99f, matchTime));
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
  }

  @Nested
  class WhenEpisodeStreamsAreAdded {
    private final Instant discoveryTime = Instant.now();
    private final Instant matchTime = Instant.now();
    private final WorkId id = new WorkId(EMDB, MediaType.SERIE, "SERIE-0001");
    private final WorkId episodeId1 = new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/01");
    private final WorkId episodeId2 = new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/02");
    private final WorkId episodeId3 = new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/03");
    private final Episode episode1 = Episodes.create(episodeId1, "eafp", 1, 1);
    private final Episode episode2 = Episodes.create(episodeId2, "tnn", 1, 2);
    private final Episode episode3 = Episodes.create(episodeId3, "coh", 1, 3);

    {

      /*
       * Scenario:
       * - Two streams exist, one claiming to be episode 1+2, and one claiming to be episode 3
       * - The external data source knows 3 episodes, but they don't match up with the streams (episode 1 is just an extended episode, and the stream for episode 3 is really episode 2).
       */

      RESOURCE_EVENTS.push(new ResourceEvent.Updated(new Resource(
        Path.of("//Server/Series/Star Trek").toUri(),
        Optional.empty(),
        MediaType.SERIE,
        new ContentID(2010),
        Instant.now(),
        Optional.empty(),
        discoveryTime,
        new StreamTags(Set.of("cartoons")),
        Optional.empty(),
        Optional.empty(),
        List.of(),
        Attributes.of(Attribute.TITLE, "Star Trek")
      )));

      RESOURCE_EVENTS.push(new ResourceEvent.Updated(new Resource(
        Path.of("//Server/Series/Star Trek/Star.Trek.-.S01E01-02.avi").toUri(),
        Optional.of(Path.of("//Server/Series/Star Trek").toUri()),
        MediaType.EPISODE,
        new ContentID(2011),
        Instant.now(),
        Optional.empty(),
        discoveryTime,
        new StreamTags(Set.of("cartoons")),
        Optional.empty(),
        Optional.empty(),
        List.of(),
        Attributes.of(Attribute.TITLE, "Star Trek")
      )));

      RESOURCE_EVENTS.push(new ResourceEvent.Updated(new Resource(
        Path.of("//Server/Series/Star Trek/Star.Trek.-.S01E03.avi").toUri(),
        Optional.of(Path.of("//Server/Series/Star Trek").toUri()),
        MediaType.EPISODE,
        new ContentID(2012),
        Instant.now(),
        Optional.empty(),
        discoveryTime,
        new StreamTags(Set.of("cartoons")),
        Optional.empty(),
        Optional.empty(),
        List.of(),
        Attributes.of(Attribute.TITLE, "Star Trek")
      )));

      IDENTIFICATION_EVENTS.push(new IdentificationEvent(
        Path.of("//Server/Series/Star Trek").toUri(),
        List.of(Series.create(id, "Star Trek TNG", List.of(episode1, episode2, episode3))),
        new Match(Type.NAME, 1.0f, matchTime)
      ));

      IDENTIFICATION_EVENTS.push(new IdentificationEvent(
        Path.of("//Server/Series/Star Trek/Star.Trek.-.S01E01-02.avi").toUri(),
        List.of(episode1, episode2),
        new Match(Type.DERIVED, 1.0f, matchTime)
      ));

      IDENTIFICATION_EVENTS.push(new IdentificationEvent(
        Path.of("//Server/Series/Star Trek/Star.Trek.-.S01E03.avi").toUri(),
        List.of(episode3),
        new Match(Type.DERIVED, 1.0f, matchTime)
      ));
    }

    @Test
    void findShouldFindIndividualEpisodes() {
      await().untilAsserted(() -> {
        LinkedWork work1 = linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/01")).orElseThrow();

        assertThat(work1.matchedResources()).hasSize(1);

        LinkedWork work2 = linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/02")).orElseThrow();

        assertThat(work2.matchedResources()).hasSize(1);

        LinkedWork work3 = linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/03")).orElseThrow();

        assertThat(work3.matchedResources()).hasSize(1);

        assertThat(work1.matchedResources()).isEqualTo(work2.matchedResources());
        assertThat(work1.matchedResources()).isNotEqualTo(work3.matchedResources());
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
      {
        RESOURCE_EVENTS.push(new ResourceEvent.Updated(new Resource(
          Path.of("//Server/Series/Star Trek/Star.Trek.-.S01E01.avi").toUri(),
          Optional.of(Path.of("//Server/Series/Star Trek").toUri()),
          MediaType.EPISODE,
          new ContentID(2011),  // content id stays the same, it was renamed
          Instant.now(),
          Optional.empty(),
          discoveryTime,
          new StreamTags(Set.of("cartoons")),
          Optional.empty(),
          Optional.empty(),
          List.of(),
          Attributes.of(Attribute.TITLE, "Star Trek")
        )));

        RESOURCE_EVENTS.push(new ResourceEvent.Updated(new Resource(
          Path.of("//Server/Series/Star Trek/Star.Trek.-.S01E02.avi").toUri(),
          Optional.of(Path.of("//Server/Series/Star Trek").toUri()),
          MediaType.EPISODE,
          new ContentID(2012),  // content id stays the same, it was renamed
          Instant.now(),
          Optional.empty(),
          discoveryTime,
          new StreamTags(Set.of("cartoons")),
          Optional.empty(),
          Optional.empty(),
          List.of(),
          Attributes.of(Attribute.TITLE, "Star Trek")
        )));

        IDENTIFICATION_EVENTS.push(new IdentificationEvent(
          Path.of("//Server/Series/Star Trek/Star.Trek.-.S01E01.avi").toUri(),
          List.of(episode1),
          new Match(Type.DERIVED, 1.0f, matchTime)
        ));

        IDENTIFICATION_EVENTS.push(new IdentificationEvent(
          Path.of("//Server/Series/Star Trek/Star.Trek.-.S01E02.avi").toUri(),
          List.of(episode2),
          new Match(Type.DERIVED, 1.0f, matchTime)
        ));

        RESOURCE_EVENTS.push(new ResourceEvent.Removed(Path.of("//Server/Series/Star Trek/Star.Trek.-.S01E01-02.avi").toUri()));
        RESOURCE_EVENTS.push(new ResourceEvent.Removed(Path.of("//Server/Series/Star Trek/Star.Trek.-.S01E03.avi").toUri()));
      }

      @Test
      void findShouldFindIndividualEpisodes() {
        await().untilAsserted(() -> {
          LinkedWork work1 = linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/01")).orElseThrow();

          assertThat(work1.matchedResources()).hasSize(1);

          LinkedWork work2 = linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/02")).orElseThrow();

          assertThat(work2.matchedResources()).hasSize(1);

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
