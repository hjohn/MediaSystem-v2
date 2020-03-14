package hs.mediasystem.db;

import hs.ddif.core.Injector;
import hs.ddif.core.JustInTimeDiscoveryPolicy;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.util.AnnotationDescriptor;
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
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamPrint;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamPrintProvider;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.services.AbstractIdentificationService;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.mediamanager.Episodes;
import hs.mediasystem.mediamanager.Movies;
import hs.mediasystem.mediamanager.Series;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.mediamanager.StreamTags;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.StringURI;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zonky.test.db.postgres.embedded.DatabasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

public class DatabaseIT {
  private static String databaseURL;

  @RegisterExtension
  public static PreparedDbExtension pg = EmbeddedPostgresExtension.preparedDatabase(new DatabasePreparer() {
    @SuppressWarnings("resource")
    @Override
    public void prepare(javax.sql.DataSource ds) throws SQLException {
      try(Connection connection = ds.getConnection()) {
        connection.prepareStatement("CREATE DATABASE mediasystem_test").execute();
        connection.prepareStatement("CREATE ROLE testuser superuser CREATEDB LOGIN PASSWORD 'test'").execute();

        String url = connection.getMetaData().getURL();

        databaseURL = url.substring(0, url.lastIndexOf('/')) + "/mediasystem_test";
      }
    }
  });

  private static StreamCacheUpdateService updateService;
  private static WorkService workService;
  private static WorksService worksService;
  private static StreamPrintProvider streamPrintProvider;
  private static ImportSourceProvider importSourceProvider;
  private static StreamPrint streamPrint1;
  private static StreamPrint streamPrint2;
  private static StreamPrint streamPrint3;
  private static StreamPrint streamPrint4;
  private static StreamPrint streamPrint5;
  private static StreamPrint streamPrint6;

  @BeforeAll
  static void beforeAll() throws SecurityException, IOException, BeanResolutionException {
    Injector injector = new Injector(new JustInTimeDiscoveryPolicy());

    injector.registerInstance("org.postgresql.Driver", AnnotationDescriptor.named("database.driverClass"));
    injector.registerInstance(databaseURL, AnnotationDescriptor.named("database.url"));
    injector.registerInstance("testuser", AnnotationDescriptor.named("database.user"));
    injector.registerInstance("test", AnnotationDescriptor.named("database.password"));
    injector.registerInstance("SET search_path = public", AnnotationDescriptor.named("database.postConnectSql"));
    injector.registerInstance("testdata", AnnotationDescriptor.named("general.basedir"));

    injector.register(MovieIdentificationService.class);
    injector.register(MovieQueryService.class);
    injector.register(SerieIdentificationService.class);
    injector.register(SerieQueryService.class);
    injector.register(EpisodeIdentificationService.class);
    injector.registerInstance(injector);

    injector.getInstance(ServiceConfigurer.class);  // Triggers service layer configuration

    updateService = injector.getInstance(StreamCacheUpdateService.class);
    workService = injector.getInstance(WorkService.class);
    worksService = injector.getInstance(WorksService.class);
    importSourceProvider = injector.getInstance(ImportSourceProvider.class);
    streamPrintProvider = injector.getInstance(StreamPrintProvider.class);

    importSourceProvider.set(List.of(
      new ImportSource(null, 1, List.of(), new StreamSource(new StreamTags(Set.of("movies")), List.of("TMDB"))),
      new ImportSource(null, 2, List.of(), new StreamSource(new StreamTags(Set.of("series")), List.of("TMDB")))
    ));

    streamPrint1 = streamPrintProvider.get(new StringURI(Paths.get("testdata/movies/Terminator.txt").toUri().toString()), 100L, 200L);
    streamPrint2 = streamPrintProvider.get(new StringURI(Paths.get("testdata/movies/Avatar.txt").toUri().toString()), 101L, 201L);
    streamPrint3 = streamPrintProvider.get(new StringURI(Paths.get("testdata/movies/Matrix.txt").toUri().toString()), 102L, 202L);
    streamPrint4 = streamPrintProvider.get(new StringURI(Paths.get("testdata/series/Friends").toUri().toString()), null, 400L);
    streamPrint5 = streamPrintProvider.get(new StringURI(Paths.get("testdata/series/Friends/friends_1x01.txt").toUri().toString()), 301L, 401L);
    streamPrint6 = streamPrintProvider.get(new StringURI(Paths.get("testdata/series/Friends/friends_1x02.txt").toUri().toString()), 302L, 402L);
  }

  private static final MediaType MOVIE = MediaType.of("MOVIE");
  private static final MediaType SERIE = MediaType.of("SERIE");
  private static final MediaType EPISODE = MediaType.of("EPISODE");
  private static final DataSource MOVIE_DS = DataSource.instance(MOVIE, "TMDB");
  private static final DataSource SERIE_DS = DataSource.instance(SERIE, "TMDB");
  private static final DataSource EPISODE_DS = DataSource.instance(EPISODE, "TMDB");

  public static class MovieIdentificationService extends AbstractIdentificationService {
    public MovieIdentificationService() {
      super(MOVIE_DS);
    }

    @Override
    public Optional<Identification> identify(Streamable streamable, MediaDescriptor parent) {
      return Optional.of(new Identification(List.of(new ProductionIdentifier(MOVIE_DS, "T" + streamable.getId().asInt())), new Match(Type.NAME_AND_RELEASE_DATE, 0.8f, Instant.now())));
    }
  }

  public static class SerieIdentificationService extends AbstractIdentificationService {
    public SerieIdentificationService() {
      super(SERIE_DS);
    }

    @Override
    public Optional<Identification> identify(Streamable streamable, MediaDescriptor parent) {
      return Optional.of(new Identification(List.of(new ProductionIdentifier(SERIE_DS, "S" + streamable.getId().asInt())), new Match(Type.NAME_AND_RELEASE_DATE, 0.8f, Instant.now())));
    }
  }

  public static class EpisodeIdentificationService extends AbstractIdentificationService {
    public EpisodeIdentificationService() {
      super(EPISODE_DS);
    }

    @Override
    public Optional<Identification> identify(Streamable streamable, MediaDescriptor parent) {
      return Optional.of(new Identification(List.of(new ProductionIdentifier(EPISODE_DS, "S4/E" + streamable.getId().asInt())), new Match(Type.NAME_AND_RELEASE_DATE, 0.8f, Instant.now())));
    }
  }

  public static class MovieQueryService extends AbstractQueryService {
    public MovieQueryService() {
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

  public static class SerieQueryService extends AbstractQueryService {
    public SerieQueryService() {
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
  class WhenASerieIsAdded extends AddFriends {
    @Test
    void findShouldFindFriends() {
      Work work = workService.find(new WorkId(SERIE_DS, "S4")).get();

      assertEquals("Friends", work.getDescriptor().getDetails().getName());
      assertEquals(1, work.getStreams().size());
    }

    @Test
    void findShouldFindFriendsEp1() {
      Work work = workService.find(new WorkId(EPISODE_DS, "S4/E5")).get();

      assertEquals("Forever", work.getDescriptor().getDetails().getName());
      assertEquals(1, work.getStreams().size());
    }

    @Test
    void findShouldFindFriendsEp2() {
      Work work = workService.find(new WorkId(EPISODE_DS, "S4/E6")).get();

      assertEquals("Never", work.getDescriptor().getDetails().getName());
      assertEquals(1, work.getStreams().size());
    }

    @Test
    void findShouldFindFriendsEp3WithoutStreamEquivalent() {
      Work work = workService.find(new WorkId(EPISODE_DS, "S4/E7")).get();

      assertEquals("Not Included", work.getDescriptor().getDetails().getName());
      assertEquals(0, work.getStreams().size());
    }

    @Test
    void findAllByTypeSerieShouldFindAll() {
      assertEquals(1, worksService.findAllByType(SERIE, "series").size());
    }
  }

  @Nested
  class WhenSomeMoviesAreAdded extends AddSomeMovies {
    @Test
    void findShouldFindTerminator() {
      Work work = workService.find(new WorkId(MOVIE_DS, "T1")).get();

      assertEquals("The Terminator", work.getDescriptor().getDetails().getName());
      assertEquals(1, work.getStreams().size());
    }

    @Test
    void findShouldFindAvatar() {
      Work work = workService.find(new WorkId(MOVIE_DS, "T2")).get();

      assertEquals("Avatar", work.getDescriptor().getDetails().getName());
      assertEquals(1, work.getStreams().size());
    }

    @Test
    void findShouldFindMatrix() {
      Work work = workService.find(new WorkId(MOVIE_DS, "T3")).get();

      assertEquals("The Matrix", work.getDescriptor().getDetails().getName());
      assertEquals(1, work.getStreams().size());
    }

    @Test
    void findAllByTypeMovieShouldFindAll() {
      assertEquals(3, worksService.findAllByType(MOVIE, "movies").size());
    }

    @Test
    void findAllByTypeNotMovieShouldNotFindAnything() {
      assertEquals(0, worksService.findAllByType(MOVIE, "non-existing").size());
    }

    @Test
    void findNewestShouldFindThem() {
      assertEquals(3, worksService.findNewest(10).size());
    }

    @Nested
    class WhenAvatarIsGoneAndTerminatorIsRenamed extends RemoveAvatarAndModifyOne {
      @Test
      void findShouldFindTerminatorStillAlthoughItsTitleChanged() {
        Work work = workService.find(new WorkId(MOVIE_DS, "T1")).get();

        assertEquals("The Terminator", work.getDescriptor().getDetails().getName());
        assertEquals(1, work.getStreams().size());
      }

      @Test
      void findShouldFindAvatarStillButWithoutAStream() {
        Work work = workService.find(new WorkId(MOVIE_DS, "T2")).get();

        assertEquals("Avatar", work.getDescriptor().getDetails().getName());
        assertEquals(0, work.getStreams().size());
      }

      @Test
      void findShouldFindMatrixStill() {
        Work work = workService.find(new WorkId(MOVIE_DS, "T3")).get();

        assertEquals("The Matrix", work.getDescriptor().getDetails().getName());
        assertEquals(1, work.getStreams().size());
      }

      @Test
      void findAllByTypeMovieShouldFindOneLess() {
        assertEquals(2, worksService.findAllByType(MOVIE, "movies").size());
      }

      @Test
      void findAllByTypeNotMovieShouldNotFindAnythingStill() {
        assertEquals(0, worksService.findAllByType(MOVIE, "non-existing").size());
      }

      @Test
      void findNewestShouldFindOneLess() {
        assertEquals(2, worksService.findNewest(10).size());
      }
    }
  }

  static class AddSomeMovies {
    @BeforeAll
    static void beforeAll() throws InterruptedException {
      updateService.update(1, List.of(Exceptional.of(List.of(
        streamable(MOVIE, "testdata/movies/Terminator.txt", streamPrint1.getId(), "Terminator"),
        streamable(MOVIE, "testdata/movies/Avatar.txt", streamPrint2.getId(), "Avatar"),
        streamable(MOVIE, "testdata/movies/Matrix.txt", streamPrint3.getId(), "The Matrix")
      ))));

      Thread.sleep(500);
      System.out.println("... hoping all have been enriched now ...");
    }

    @AfterAll
    static void afterAll() {
      updateService.update(1, List.of(Exceptional.of(List.of())));  // Should remove everything
    }
  }

  static class RemoveAvatarAndModifyOne {
    @BeforeAll
    static void beforeAll() throws InterruptedException {
      updateService.update(1, List.of(Exceptional.of(List.of(
        streamable(MOVIE, "testdata/movies/Terminator.txt", streamPrint1.getId(), "The Terminator"),
        streamable(MOVIE, "testdata/movies/Matrix.txt", streamPrint3.getId(), "The Matrix")
      ))));

      Thread.sleep(500);
      System.out.println("... hoping all have been enriched now ...");
    }
  }

  static class AddFriends {
    @BeforeAll
    static void beforeAll() throws InterruptedException {
      updateService.update(2, List.of(Exceptional.of(List.of(
        streamable(SERIE, "testdata/series/Friends", streamPrint4.getId(), "Friends"),
        streamable(EPISODE, "testdata/series/Friends/friends_1x01.txt", streamPrint5.getId(), streamPrint4.getId(), "1x01"),
        streamable(EPISODE, "testdata/series/Friends/friends_1x02.txt", streamPrint6.getId(), streamPrint4.getId(), "1x02")
      ))));

      Thread.sleep(500);
      System.out.println("... hoping all have been enriched now ...");
    }

    @AfterAll
    static void afterAll() {
      updateService.update(2, List.of(Exceptional.of(List.of())));  // Should remove everything
    }
  }

  private static Streamable streamable(MediaType type, String uri, StreamID sid, String title) {
    return new Streamable(type, new StringURI(uri), sid, null, Attributes.of(Attribute.TITLE, title));
  }

  private static Streamable streamable(MediaType type, String uri, StreamID sid, StreamID pid, String title) {
    return new Streamable(type, new StringURI(uri), sid, pid, Attributes.of(Attribute.TITLE, title));
  }
}
