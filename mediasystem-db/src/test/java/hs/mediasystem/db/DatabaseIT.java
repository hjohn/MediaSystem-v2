package hs.mediasystem.db;

import hs.ddif.annotations.Produces;
import hs.mediasystem.db.base.ImportSource;
import hs.mediasystem.db.base.ImportSourceProvider;
import hs.mediasystem.db.base.StreamCacheUpdateService;
import hs.mediasystem.db.services.WorkService;
import hs.mediasystem.db.services.WorksService;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.EpisodeIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrint;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrintProvider;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.services.AbstractIdentificationService;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.mediamanager.Episodes;
import hs.mediasystem.mediamanager.Movies;
import hs.mediasystem.mediamanager.Series;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.mediamanager.StreamTags;
import hs.mediasystem.skipscan.db.DatabaseConfig;
import hs.mediasystem.util.Attributes;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(InjectorExtension.class)
public class DatabaseIT extends DatabaseConfig {
  private static final DataSource MOVIE_DS = DataSource.instance(MediaType.MOVIE, "TMDB");
  private static final DataSource SERIE_DS = DataSource.instance(MediaType.SERIE, "TMDB");
  private static final DataSource EPISODE_DS = DataSource.instance(MediaType.EPISODE, "TMDB");

  @Inject private StreamCacheUpdateService updateService;
  @Inject private WorkService workService;
  @Inject private WorksService worksService;
  @Inject private ContentPrintProvider contentPrintProvider;
  @Inject private ImportSourceProvider importSourceProvider;
  @Inject @Named("general.basedir") private String basePath;

  private ContentPrint contentPrint1;
  private ContentPrint contentPrint2;
  private ContentPrint contentPrint3;
  private ContentPrint contentPrint4;
  private ContentPrint contentPrint5;
  private ContentPrint contentPrint6;

  @PostConstruct
  private void postConstruct() throws IOException {
    contentPrint1 = contentPrintProvider.get(Paths.get(basePath + "/movies/Terminator.txt").toUri(), 100L, 200L);
    contentPrint2 = contentPrintProvider.get(Paths.get(basePath + "/movies/Avatar.txt").toUri(), 101L, 201L);
    contentPrint3 = contentPrintProvider.get(Paths.get(basePath + "/movies/Matrix.txt").toUri(), 102L, 202L);
    contentPrint4 = contentPrintProvider.get(Paths.get(basePath + "/series/Friends").toUri(), null, 400L);
    contentPrint5 = contentPrintProvider.get(Paths.get(basePath + "/series/Friends/friends_1x01.txt").toUri(), 301L, 401L);
    contentPrint6 = contentPrintProvider.get(Paths.get(basePath + "/series/Friends/friends_1x02.txt").toUri(), 302L, 402L);

    importSourceProvider.set(List.of(
      new ImportSource(null, 1, null, new StreamSource(new StreamTags(Set.of("movies")), List.of("TMDB"))),
      new ImportSource(null, 2, null, new StreamSource(new StreamTags(Set.of("series")), List.of("TMDB")))
    ));
  }

  /*
   * Create a few services that normally are provided by scanned plug-ins:
   */

  @Produces private static final MovieIdentificationService MOVIE_IDENTIFICATION_SERVICE = new MovieIdentificationService();
  @Produces private static final SerieIdentificationService SERIE_IDENTIFICATION_SERVICE = new SerieIdentificationService();
  @Produces private static final EpisodeIdentificationService EPISODE_IDENTIFICATION_SERVICE = new EpisodeIdentificationService();
  @Produces private static final MovieQueryService MOVIE_QUERY_SERVICE = new MovieQueryService();
  @Produces private static final SerieQueryService SERIE_QUERY_SERVICE = new SerieQueryService();

  static class MovieIdentificationService extends AbstractIdentificationService {
    MovieIdentificationService() {
      super(MOVIE_DS);
    }

    @Override
    public Optional<Identification> identify(Streamable streamable, MediaDescriptor parent) {
      return Optional.of(new Identification(List.of(new ProductionIdentifier(MOVIE_DS, "T" + streamable.getId().getContentId().asInt())), new Match(Type.NAME_AND_RELEASE_DATE, 0.8f, Instant.now())));
    }
  }

  static class SerieIdentificationService extends AbstractIdentificationService {
    SerieIdentificationService() {
      super(SERIE_DS);
    }

    @Override
    public Optional<Identification> identify(Streamable streamable, MediaDescriptor parent) {
      return Optional.of(new Identification(List.of(new ProductionIdentifier(SERIE_DS, "S" + streamable.getId().getContentId().asInt())), new Match(Type.NAME_AND_RELEASE_DATE, 0.8f, Instant.now())));
    }
  }

  static class EpisodeIdentificationService extends AbstractIdentificationService {
    EpisodeIdentificationService() {
      super(EPISODE_DS);
    }

    @Override
    public Optional<Identification> identify(Streamable streamable, MediaDescriptor parent) {
      return Optional.of(new Identification(List.of(new ProductionIdentifier(EPISODE_DS, "S4/E" + streamable.getId().getContentId().asInt())), new Match(Type.NAME_AND_RELEASE_DATE, 0.8f, Instant.now())));
    }
  }

  static class MovieQueryService extends AbstractQueryService {
    MovieQueryService() {
      super(MOVIE_DS);
    }

    @Override
    public MediaDescriptor query(Identifier identifier) {
      switch(identifier.toString()) {
      case "TMDB:MOVIE:T1": return Movies.create((ProductionIdentifier)identifier, "The Terminator");
      case "TMDB:MOVIE:T2": return Movies.create((ProductionIdentifier)identifier, "Avatar");
      case "TMDB:MOVIE:T3": return Movies.create((ProductionIdentifier)identifier, "The Matrix");
      default:
        throw new IllegalStateException("Unknown identifier: " + identifier);
      }
    }
  }

  static class SerieQueryService extends AbstractQueryService {
    SerieQueryService() {
      super(SERIE_DS);
    }

    @Override
    public MediaDescriptor query(Identifier identifier) {
      switch(identifier.toString()) {
      case "TMDB:SERIE:S4": return Series.create((ProductionIdentifier)identifier, "Friends", List.of(
        Episodes.create(new EpisodeIdentifier(EPISODE_DS, "S4/E5"), "Forever", 1, 1),
        Episodes.create(new EpisodeIdentifier(EPISODE_DS, "S4/E6"), "Never", 1, 2),
        Episodes.create(new EpisodeIdentifier(EPISODE_DS, "S4/E7"), "Not Included", 1, 3)
      ));
      default:
        throw new IllegalStateException("Unknown identifier: " + identifier);
      }
    }
  }

  @Nested
  @TestInstance(Lifecycle.PER_CLASS)
  class WhenASerieIsAdded extends AddFriends {
    @Test
    void findShouldFindFriends() {
      Work work = workService.find(new WorkId(SERIE_DS, "S4")).get();

      assertEquals("Friends", work.getDescriptor().getDetails().getTitle());
      assertEquals(1, work.getStreams().size());
    }

    @Test
    void findShouldFindFriendsEp1() {
      Work work = workService.find(new WorkId(EPISODE_DS, "S4/E5")).get();

      assertEquals("Forever", work.getDescriptor().getDetails().getTitle());
      assertEquals(1, work.getStreams().size());
    }

    @Test
    void findShouldFindFriendsEp2() {
      Work work = workService.find(new WorkId(EPISODE_DS, "S4/E6")).get();

      assertEquals("Never", work.getDescriptor().getDetails().getTitle());
      assertEquals(1, work.getStreams().size());
    }

    @Test
    void findShouldFindFriendsEp3WithoutStreamEquivalent() {
      Work work = workService.find(new WorkId(EPISODE_DS, "S4/E7")).get();

      assertEquals("Not Included", work.getDescriptor().getDetails().getTitle());
      assertEquals(0, work.getStreams().size());
    }

    @Test
    void findAllByTypeSerieShouldFindAll() {
      assertEquals(1, worksService.findAllByType(MediaType.SERIE, "series").size());
    }
  }

  @Nested
  @TestInstance(Lifecycle.PER_CLASS)
  class WhenSomeMoviesAreAdded extends AddSomeMovies {
    @Test
    void findShouldFindTerminator() {
      Work work = workService.find(new WorkId(MOVIE_DS, "T1")).get();

      assertEquals("The Terminator", work.getDescriptor().getDetails().getTitle());
      assertEquals(1, work.getStreams().size());
    }

    @Test
    void findShouldFindAvatar() {
      Work work = workService.find(new WorkId(MOVIE_DS, "T2")).get();

      assertEquals("Avatar", work.getDescriptor().getDetails().getTitle());
      assertEquals(1, work.getStreams().size());
    }

    @Test
    void findShouldFindMatrix() {
      Work work = workService.find(new WorkId(MOVIE_DS, "T3")).get();

      assertEquals("The Matrix", work.getDescriptor().getDetails().getTitle());
      assertEquals(1, work.getStreams().size());
    }

    @Test
    void findAllByTypeMovieShouldFindAll() {
      assertEquals(3, worksService.findAllByType(MediaType.MOVIE, "movies").size());
    }

    @Test
    void findAllByTypeNotMovieShouldNotFindAnything() {
      assertEquals(0, worksService.findAllByType(MediaType.MOVIE, "non-existing").size());
    }

    @Test
    void findNewestShouldFindThem() {
      assertEquals(3, worksService.findNewest(10, m -> true).size());
    }

    @Nested
    @TestInstance(Lifecycle.PER_CLASS)
    class WhenAvatarIsGoneAndTerminatorIsRenamed extends RemoveAvatarAndModifyOne {
      @Test
      void findShouldFindTerminatorStillAlthoughItsTitleChanged() {
        Work work = workService.find(new WorkId(MOVIE_DS, "T1")).get();

        assertEquals("The Terminator", work.getDescriptor().getDetails().getTitle());
        assertEquals(1, work.getStreams().size());
      }

      @Test
      void findShouldFindAvatarStillButWithoutAStream() {
        Work work = workService.find(new WorkId(MOVIE_DS, "T2")).get();

        assertEquals("Avatar", work.getDescriptor().getDetails().getTitle());
        assertEquals(0, work.getStreams().size());
      }

      @Test
      void findShouldFindMatrixStill() {
        Work work = workService.find(new WorkId(MOVIE_DS, "T3")).get();

        assertEquals("The Matrix", work.getDescriptor().getDetails().getTitle());
        assertEquals(1, work.getStreams().size());
      }

      @Test
      void findAllByTypeMovieShouldFindOneLess() {
        assertEquals(2, worksService.findAllByType(MediaType.MOVIE, "movies").size());
      }

      @Test
      void findAllByTypeNotMovieShouldNotFindAnythingStill() {
        assertEquals(0, worksService.findAllByType(MediaType.MOVIE, "non-existing").size());
      }

      @Test
      void findNewestShouldFindOneLess() {
        assertEquals(2, worksService.findNewest(10, x -> true).size());
      }
    }
  }

  class AddSomeMovies {

    @BeforeAll
    void beforeAll() throws InterruptedException {
      updateService.update(1, List.of(
        streamable(MediaType.MOVIE, "testdata/movies/Terminator.txt", new StreamID(1, contentPrint1.getId(), "Terminator"), "Terminator"),
        streamable(MediaType.MOVIE, "testdata/movies/Avatar.txt", new StreamID(1, contentPrint2.getId(), "Avatar"), "Avatar"),
        streamable(MediaType.MOVIE, "testdata/movies/Matrix.txt", new StreamID(1, contentPrint3.getId(), "The Matrix"), "The Matrix")
      ));

      Thread.sleep(500);
      System.out.println("... hoping all have been enriched now ...");
    }

    @AfterAll
    void afterAll() {
      updateService.update(1, List.of());  // Should remove everything
    }
  }

  class RemoveAvatarAndModifyOne {

    @BeforeAll
    void beforeAll() throws InterruptedException {
      updateService.update(1, List.of(
        streamable(MediaType.MOVIE, "testdata/movies/Terminator.txt", new StreamID(1, contentPrint1.getId(), "The Terminator"), "The Terminator"),
        streamable(MediaType.MOVIE, "testdata/movies/Matrix.txt", new StreamID(1, contentPrint3.getId(), "The Matrix"), "The Matrix")
      ));

      Thread.sleep(500);
      System.out.println("... hoping all have been enriched now ...");
    }
  }

  class AddFriends {

    @BeforeAll
    void beforeAll() throws InterruptedException {
      StreamID sid = new StreamID(2, contentPrint4.getId(), "Friends");

      updateService.update(2, List.of(
        streamable(MediaType.SERIE, "testdata/series/Friends", sid, "Friends"),
        streamable(MediaType.EPISODE, "testdata/series/Friends/friends_1x01.txt", new StreamID(2, contentPrint5.getId(), "1x01"), sid, "1x01"),
        streamable(MediaType.EPISODE, "testdata/series/Friends/friends_1x02.txt", new StreamID(2, contentPrint6.getId(), "1x02"), sid, "1x02")
      ));

      Thread.sleep(500);
      System.out.println("... hoping all have been enriched now ...");
    }

    @AfterAll
    void afterAll() {
      updateService.update(2, List.of());  // Should remove everything
    }
  }

  private static Streamable streamable(MediaType type, String uri, StreamID sid, String title) {
    return new Streamable(type, URI.create(uri), sid, null, Attributes.of(Attribute.TITLE, title));
  }

  private static Streamable streamable(MediaType type, String uri, StreamID sid, StreamID pid, String title) {
    return new Streamable(type, URI.create(uri), sid, pid, Attributes.of(Attribute.TITLE, title));
  }
}
