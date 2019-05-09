package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identification.MatchType;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.Tuple;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class StreamStoreTest {
  private static final Identification IDENTIFICATION = new Identification(MatchType.ID, 1.0, Instant.now());
  private static final StreamTags STREAM_TAGS = new StreamTags(Set.of("A", "B"));

  @Mock private EpisodeMatcher episodeMatcher;
  @InjectMocks private StreamStore store;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void shouldFindNewlyAddedStream() {
    Movie movie = Movies.create();
    BasicStream stream = Streams.create();

    store.put(stream, STREAM_TAGS, Set.of(Tuple.of(movie.getIdentifier(), IDENTIFICATION, movie)));

    assertEquals(Set.of(stream), store.findStreams(movie.getIdentifier()));

    store.remove(stream);

    assertEquals(Set.of(), store.findStreams(movie.getIdentifier()));
  }

  @Test
  public void addSerieWithEpisodesShouldFindAllCorrectly() {
    Episode episode1 = Episodes.create("0001", 1, 5);
    Episode episode2 = Episodes.create("0002", 1, 6);
    Serie serie = Series.create(List.of(episode1, episode2));
    BasicStream episode1Stream = Streams.create(MediaType.of("EPISODE"), "http://serie/ep01.avi", StreamPrints.create(new StreamID(11)), "1,5");
    BasicStream episode2Stream = Streams.create(MediaType.of("EPISODE"), "http://serie/ep02.avi", StreamPrints.create(new StreamID(12)), "1,6");
    BasicStream serieStream = Streams.create("http://serie", StreamPrints.create(new StreamID(1)), List.of(episode1Stream, episode2Stream));

    when(episodeMatcher.attemptMatch(serie, IDENTIFICATION, episode1Stream.getAttributes())).thenReturn(Tuple.of(IDENTIFICATION, List.of(episode1)));
    when(episodeMatcher.attemptMatch(serie, IDENTIFICATION, episode2Stream.getAttributes())).thenReturn(Tuple.of(IDENTIFICATION, List.of(episode2)));

    store.put(serieStream, STREAM_TAGS, Set.of(Tuple.of(serie.getIdentifier(), IDENTIFICATION, serie)));

    assertEquals(Set.of(serieStream), store.findStreams(serie.getIdentifier()));
    assertEquals(Set.of(episode1Stream), store.findStreams(episode1.getIdentifier()));
    assertEquals(Set.of(episode2Stream), store.findStreams(episode2.getIdentifier()));

    store.remove(serieStream);

    assertEquals(Set.of(), store.findStreams(serie.getIdentifier()));
    assertEquals(Set.of(), store.findStreams(episode1.getIdentifier()));
    assertEquals(Set.of(), store.findStreams(episode2.getIdentifier()));
  }

  @Test
  public void addSerieThenEpisodesWithDescriptorsShouldFindAllCorrectly() {
    Episode episode1 = Episodes.create("0001", 1, 5);
    Episode episode2 = Episodes.create("0002", 1, 6);
    Serie serie = Series.create(List.of(episode1, episode2));
    BasicStream serieStream = Streams.create("http://serie", StreamPrints.create(new StreamID(1)));

    store.put(serieStream, STREAM_TAGS, Set.of(Tuple.of(serie.getIdentifier(), IDENTIFICATION, serie)));

    assertEquals(Set.of(serieStream), store.findStreams(serie.getIdentifier()));

    BasicStream episode1Stream = Streams.create(MediaType.of("EPISODE"), "http://serie/ep01.avi", StreamPrints.create(new StreamID(11)), "1,5");

    store.put(episode1Stream, STREAM_TAGS, Set.of(Tuple.of(episode1.getIdentifier(), IDENTIFICATION, episode1)));

    assertEquals(Set.of(serieStream), store.findStreams(serie.getIdentifier()));
    assertEquals(Set.of(episode1Stream), store.findStreams(episode1.getIdentifier()));

    BasicStream episode2Stream = Streams.create(MediaType.of("EPISODE"), "http://serie/ep02.avi", StreamPrints.create(new StreamID(12)), "1,6");

    store.put(episode2Stream, STREAM_TAGS, Set.of(Tuple.of(episode2.getIdentifier(), IDENTIFICATION, episode2)));

    assertEquals(Set.of(serieStream), store.findStreams(serie.getIdentifier()));
    assertEquals(Set.of(episode1Stream), store.findStreams(episode1.getIdentifier()));
    assertEquals(Set.of(episode2Stream), store.findStreams(episode2.getIdentifier()));

    store.remove(serieStream);

    assertEquals(Set.of(), store.findStreams(serie.getIdentifier()));
    assertEquals(Set.of(episode1Stream), store.findStreams(episode1.getIdentifier()));
    assertEquals(Set.of(episode2Stream), store.findStreams(episode2.getIdentifier()));

    store.remove(episode1Stream);

    assertEquals(Set.of(), store.findStreams(serie.getIdentifier()));
    assertEquals(Set.of(), store.findStreams(episode1.getIdentifier()));
    assertEquals(Set.of(episode2Stream), store.findStreams(episode2.getIdentifier()));

    store.remove(episode2Stream);

    assertEquals(Set.of(), store.findStreams(serie.getIdentifier()));
    assertEquals(Set.of(), store.findStreams(episode1.getIdentifier()));
    assertEquals(Set.of(), store.findStreams(episode2.getIdentifier()));
  }

  @Test
  public void addEpisodesWithDescriptorsThenSerieShouldFindAllCorrectly() {
    Episode episode1 = Episodes.create("0001", 1, 5);
    Episode episode2 = Episodes.create("0002", 1, 6);
    Serie serie = Series.create(List.of(episode1, episode2));
    BasicStream episode1Stream = Streams.create(MediaType.of("EPISODE"), "http://serie/ep01.avi", StreamPrints.create(new StreamID(11)), "1,5");

    store.put(episode1Stream, STREAM_TAGS, Set.of(Tuple.of(episode1.getIdentifier(), IDENTIFICATION, episode1)));

    assertEquals(Set.of(episode1Stream), store.findStreams(episode1.getIdentifier()));

    BasicStream episode2Stream = Streams.create(MediaType.of("EPISODE"), "http://serie/ep02.avi", StreamPrints.create(new StreamID(12)), "1,6");

    store.put(episode2Stream, STREAM_TAGS, Set.of(Tuple.of(episode2.getIdentifier(), IDENTIFICATION, episode2)));

    assertEquals(Set.of(episode1Stream), store.findStreams(episode1.getIdentifier()));
    assertEquals(Set.of(episode2Stream), store.findStreams(episode2.getIdentifier()));

    BasicStream serieStream = Streams.create("http://serie", StreamPrints.create(new StreamID(1)));

    store.put(serieStream, STREAM_TAGS, Set.of(Tuple.of(serie.getIdentifier(), IDENTIFICATION, serie)));

    assertEquals(Set.of(serieStream), store.findStreams(serie.getIdentifier()));
    assertEquals(Set.of(episode1Stream), store.findStreams(episode1.getIdentifier()));
    assertEquals(Set.of(episode2Stream), store.findStreams(episode2.getIdentifier()));

    store.remove(episode1Stream);

    assertEquals(Set.of(serieStream), store.findStreams(serie.getIdentifier()));
    assertEquals(Set.of(), store.findStreams(episode1.getIdentifier()));
    assertEquals(Set.of(episode2Stream), store.findStreams(episode2.getIdentifier()));

    store.remove(episode2Stream);

    assertEquals(Set.of(serieStream), store.findStreams(serie.getIdentifier()));
    assertEquals(Set.of(), store.findStreams(episode1.getIdentifier()));
    assertEquals(Set.of(), store.findStreams(episode2.getIdentifier()));

    store.remove(serieStream);

    assertEquals(Set.of(), store.findStreams(serie.getIdentifier()));
    assertEquals(Set.of(), store.findStreams(episode1.getIdentifier()));
    assertEquals(Set.of(), store.findStreams(episode2.getIdentifier()));
  }
}
