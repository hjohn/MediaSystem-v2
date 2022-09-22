package hs.mediasystem.db.services;

import hs.ddif.annotations.Produces;
import hs.mediasystem.db.InjectorExtension;
import hs.mediasystem.db.base.CachedStream;
import hs.mediasystem.db.base.ImportSource;
import hs.mediasystem.db.base.ImportSourceProvider;
import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.db.base.StreamableEvent;
import hs.mediasystem.db.extract.StreamMetaDataEvent;
import hs.mediasystem.db.services.domain.LinkedWork;
import hs.mediasystem.db.services.domain.MatchedResource;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrint;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrintProvider;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.mediamanager.DescriptorStore;
import hs.mediasystem.mediamanager.Episodes;
import hs.mediasystem.mediamanager.Movies;
import hs.mediasystem.mediamanager.Series;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.mediamanager.StreamTags;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.events.Event;
import hs.mediasystem.util.events.InMemoryEventStream;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(InjectorExtension.class)
public class WorksServiceIT {
  @Produces
  @Singleton
  private static final InMemoryEventStream<StreamableEvent> createStream() {
    return new InMemoryEventStream<>();
  }

  @Produces private static final DescriptorStore DESCRIPTOR_STORE = mock(DescriptorStore.class);
  @Produces private static final StreamStateService STREAM_STATE_SERVICE = mock(StreamStateService.class);
  @Produces private static final ContentPrintProvider CONTENT_PRINT_PROVIDER = mock(ContentPrintProvider.class);
  @Produces private static final ImportSourceProvider IMPORT_SOURCE_PROVIDER = mock(ImportSourceProvider.class);

  @SuppressWarnings("unchecked")
  @Produces private static final InMemoryEventStream<StreamMetaDataEvent> streamMetaDataEvents = mock(InMemoryEventStream.class);

  @Inject LinkedResourcesService linkedResourcesService;  // cannot be auto-discovered as not directly used, ensure it is present

  @Inject private LinkedWorksService linkedWorksService;
  @Inject private InMemoryEventStream<StreamableEvent> streamableEvents;

  private static final DataSource INTERNAL = DataSource.instance("@INTERNAL");
  private static final DataSource EMDB = DataSource.instance("EMDB");

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
      when(IMPORT_SOURCE_PROVIDER.getImportSource(anyInt())).thenReturn(new ImportSource(null, 1, null, new StreamSource(new StreamTags(Set.of("cartoon")), List.of())));
      when(CONTENT_PRINT_PROVIDER.get(new ContentID(2001))).thenReturn(new ContentPrint(new ContentID(2001), 4002L, 8004L, new byte[32], Instant.now()));

      streamableEvents.push(new Event<>(new StreamableEvent.Updated(new CachedStream(
        new Streamable(MediaType.MOVIE, URI.create("http://here"), new StreamID(1, new ContentID(2001), "terminator.avi"), null, Attributes.of("title", "Terminator")),
        null,
        discoveryTime,
        Instant.now(),
        Instant.now()
      ))));

      sleep(100);
    }

    @Test
    void findShouldFindMovie() {
      Optional<LinkedWork> optionalWork = linkedWorksService.find(new WorkId(INTERNAL, MediaType.MOVIE, "1:2001:terminator.avi"));

      assertThat(optionalWork).isNotEmpty();

      LinkedWork linkedWork = optionalWork.orElseThrow();

      assertThat(linkedWork.work().descriptor().getDetails().getTitle()).isEqualTo("Terminator");
      assertThat(linkedWork.work().descriptor().getDetails().getCover()).isEmpty();
      assertThat(linkedWork.matchedResources()).hasSize(1);

      MatchedResource matchedResource = linkedWork.matchedResources().get(0);

      assertThat(matchedResource.resource().id()).isEqualTo(new StreamID(1, new ContentID(2001), "terminator.avi"));
      assertThat(matchedResource.match()).isEqualTo(new Match(Type.NONE, 1.0f, matchedResource.resource().discoveryTime()));
    }

    @Test
    void findAllByTypeShouldFindMovies() {
      List<LinkedWork> works = linkedWorksService.findAllByType(MediaType.MOVIE, null);

      assertThat(works).hasSize(1);
    }

    @Nested
    class AndThenEnriched {
      private final Instant matchTime = Instant.now();
      private final WorkId id = new WorkId(EMDB, MediaType.MOVIE, "T800");

      {
        when(DESCRIPTOR_STORE.find(id)).thenReturn(Optional.of(Movies.create(id, "The Terminator")));

        streamableEvents.push(new Event<>(new StreamableEvent.Updated(new CachedStream(
          new Streamable(MediaType.MOVIE, URI.create("http://here"), new StreamID(1, new ContentID(2001), "terminator.avi"), null, Attributes.of("title", "Terminator")),
          new Identification(List.of(id), new Match(Type.NAME_AND_RELEASE_DATE, 0.99f, matchTime)),
          discoveryTime,
          Instant.now(),
          Instant.now()
        ))));

        sleep(100);
      }

      @Test
      void findShouldFindEnrichedMovie() {
        Optional<LinkedWork> optionalWork = linkedWorksService.find(new WorkId(EMDB, MediaType.MOVIE, "T800"));

        assertThat(optionalWork).isNotEmpty();

        LinkedWork linkedWork = optionalWork.orElseThrow();

        assertThat(linkedWork.work().descriptor().getDetails().getTitle()).isEqualTo("The Terminator");
        assertThat(linkedWork.work().descriptor().getDetails().getCover()).hasValue(new ImageURI("http://localhost", ""));
        assertThat(linkedWork.matchedResources()).hasSize(1);

        MatchedResource matchedResource = linkedWork.matchedResources().get(0);

        assertThat(matchedResource.resource().id()).isEqualTo(new StreamID(1, new ContentID(2001), "terminator.avi"));
        assertThat(matchedResource.match()).isEqualTo(new Match(Type.NAME_AND_RELEASE_DATE, 0.99f, matchTime));
      }

      @Test
      void findAllByTypeShouldFindMovies() {
        List<LinkedWork> works = linkedWorksService.findAllByType(MediaType.MOVIE, null);

        assertThat(works).hasSize(1);
      }
    }
  }

  @Nested
  class WhenEpisodeStreamsAreAdded {
    private final Instant discoveryTime = Instant.now();
    private final Instant matchTime = Instant.now();
    private final StreamID serieStreamId = new StreamID(1, new ContentID(2010), "Star Trek");
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

      when(IMPORT_SOURCE_PROVIDER.getImportSource(anyInt())).thenReturn(new ImportSource(null, 1, null, new StreamSource(new StreamTags(Set.of("cartoon")), List.of())));
      when(CONTENT_PRINT_PROVIDER.get(new ContentID(2010))).thenReturn(new ContentPrint(new ContentID(2010), null, 8004L, new byte[32], Instant.now()));
      when(CONTENT_PRINT_PROVIDER.get(new ContentID(2011))).thenReturn(new ContentPrint(new ContentID(2011), 4002L, 8005L, new byte[32], Instant.now()));
      when(CONTENT_PRINT_PROVIDER.get(new ContentID(2012))).thenReturn(new ContentPrint(new ContentID(2012), 4003L, 8006L, new byte[32], Instant.now()));
      when(DESCRIPTOR_STORE.find(id)).thenReturn(Optional.of(Series.create(id, "Star Trek TNG", List.of(episode1, episode2, episode3))));
      when(DESCRIPTOR_STORE.find(episodeId1)).thenReturn(Optional.of(episode1));
      when(DESCRIPTOR_STORE.find(episodeId2)).thenReturn(Optional.of(episode2));
      when(DESCRIPTOR_STORE.find(episodeId3)).thenReturn(Optional.of(episode3));

      streamableEvents.push(new Event<>(new StreamableEvent.Updated(new CachedStream(
        new Streamable(MediaType.SERIE, URI.create("http://serie"), serieStreamId, null, Attributes.of("title", "Star Trek")),
        new Identification(List.of(id), new Match(Type.NAME_AND_RELEASE_DATE, 0.99f, matchTime)),
        discoveryTime,
        Instant.now(),
        Instant.now()
      ))));

      streamableEvents.push(new Event<>(new StreamableEvent.Updated(new CachedStream(
        new Streamable(MediaType.EPISODE, URI.create("http://serie/Star.Trek.-.S01E01-02.avi"), new StreamID(1, new ContentID(2011), "Star Trek - S01E01-02.avi"), serieStreamId, Attributes.of("title", "Star Trek")),
        new Identification(List.of(episodeId1, episodeId2), new Match(Type.DERIVED, 1.0f, matchTime)),
        discoveryTime,
        Instant.now(),
        Instant.now()
      ))));

      streamableEvents.push(new Event<>(new StreamableEvent.Updated(new CachedStream(
        new Streamable(MediaType.EPISODE, URI.create("http://serie/Star.Trek.-.S01E03.avi"), new StreamID(1, new ContentID(2012), "Star Trek - S01E03.avi"), serieStreamId, Attributes.of("title", "Star Trek")),
        new Identification(List.of(episodeId3), new Match(Type.DERIVED, 1.0f, matchTime)),
        discoveryTime,
        Instant.now(),
        Instant.now()
      ))));

      await().until(() -> linkedWorksService.findChildren(new WorkId(EMDB, MediaType.SERIE, "SERIE-0001")).size() == 3);
    }

    @Test
    void findShouldFindIndividualEpisodes() {
      LinkedWork work1 = linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/01")).orElseThrow();

      assertThat(work1.matchedResources()).hasSize(1);

      LinkedWork work2 = linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/02")).orElseThrow();

      assertThat(work2.matchedResources()).hasSize(1);

      LinkedWork work3 = linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/03")).orElseThrow();

      assertThat(work3.matchedResources()).hasSize(1);

      assertThat(work1.matchedResources()).isEqualTo(work2.matchedResources());
      assertThat(work1.matchedResources()).isNotEqualTo(work3.matchedResources());
    }

    @Test
    void findChildrenShouldFindChildrenOfSerie() {
      List<LinkedWork> works = linkedWorksService.findChildren(new WorkId(EMDB, MediaType.SERIE, "SERIE-0001"));

      assertThat(works).hasSize(3);
    }

    @Nested
    class AndThenRenamedToMatchExternalDataSource {
      {
        streamableEvents.push(new Event<>(new StreamableEvent.Updated(new CachedStream(
          new Streamable(MediaType.EPISODE, URI.create("http://serie/Star.Trek.-.S01E01.avi"), new StreamID(1, new ContentID(2011), "Star Trek - S01E01.avi"), serieStreamId, Attributes.of("title", "Star Trek")),
          new Identification(List.of(episodeId1), new Match(Type.DERIVED, 1.0f, matchTime)),
          discoveryTime,
          Instant.now(),
          Instant.now()
        ))));

        streamableEvents.push(new Event<>(new StreamableEvent.Updated(new CachedStream(
          new Streamable(MediaType.EPISODE, URI.create("http://serie/Star.Trek.-.S01E02.avi"), new StreamID(1, new ContentID(2012), "Star Trek - S01E02.avi"), serieStreamId, Attributes.of("title", "Star Trek")),
          new Identification(List.of(episodeId2), new Match(Type.DERIVED, 1.0f, matchTime)),
          discoveryTime,
          Instant.now(),
          Instant.now()
        ))));

        streamableEvents.push(new Event<>(new StreamableEvent.Removed(new StreamID(1, new ContentID(2011), "Star Trek - S01E01-02.avi"))));
        streamableEvents.push(new Event<>(new StreamableEvent.Removed(new StreamID(1, new ContentID(2012), "Star Trek - S01E03.avi"))));

        sleep(100);
      }

      @Test
      void findShouldFindIndividualEpisodes() {
        LinkedWork work1 = linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/01")).orElseThrow();

        assertThat(work1.matchedResources()).hasSize(1);

        LinkedWork work2 = linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/02")).orElseThrow();

        assertThat(work2.matchedResources()).hasSize(1);

        assertThat(linkedWorksService.find(new WorkId(EMDB, MediaType.EPISODE, "SERIE-0001/03"))).isEmpty();
      }

      @Test
      void findChildrenShouldFindChildrenOfSerie() {
        List<LinkedWork> works = linkedWorksService.findChildren(new WorkId(EMDB, MediaType.SERIE, "SERIE-0001"));

        assertThat(works).hasSize(2);
      }
    }
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    }
    catch(InterruptedException e) {
      throw new AssertionError(e);
    }
  }
}
