package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identification.MatchType;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.tmdb.movie.TmdbIdentificationService;
import hs.mediasystem.scanner.api.Attribute;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;
import hs.mediasystem.util.Tuple.Tuple2;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class TmdbIdentificationServiceTest {
  private ObjectMapper objectMapper = new ObjectMapper();

  @Mock private TheMovieDatabase tmdb;
  @InjectMocks private TmdbIdentificationService service;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void shouldIdentifyMovies() throws JsonProcessingException, IOException {
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("Terminator 5 Genisys"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80000,\"original_title\":\"Terminator Genisys\",\"release_date\":\"2015-07-01\",\"title\":\"Terminator Genisys\"}]}"));
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("Terminator, The 5 Genisys"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[]}"));
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("The Terminator 5 Genisys"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[]}"));

    Tuple2<Identifier, Identification> t = service.identify(basicStream(Attributes.of(Attribute.TITLE, "Terminator, The", Attribute.YEAR, "2015", Attribute.SUBTITLE, "Genisys", Attribute.SEQUENCE, "5")));

    assertEquals(0.98, t.b.getMatchAccuracy(), 0.01f);
    assertEquals(MatchType.NAME_AND_RELEASE_DATE, t.b.getMatchType());
    assertEquals(new Identifier(DataSources.TMDB_MOVIE, "80000"), t.a);
  }

  @Test
  public void shouldIdentifyMovies2() throws JsonProcessingException, IOException {
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("Michiel de Ruyter"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"original_title\":\"Michiel de Ruyter\",\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));

    Tuple2<Identifier, Identification> t = service.identify(basicStream(Attributes.of(Attribute.TITLE, "Michiel de Ruyter", Attribute.YEAR, "2015")));

    assertEquals(1.0, t.b.getMatchAccuracy(), 0.01f);
    assertEquals(MatchType.NAME_AND_RELEASE_DATE, t.b.getMatchType());
    assertEquals(new Identifier(DataSources.TMDB_MOVIE, "80001"), t.a);
  }

  @Test
  public void shouldNotFailWhenNoOriginalTitlePresent() throws JsonProcessingException, IOException {
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("Michiel de Ruyter"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));

    Tuple2<Identifier, Identification> t = service.identify(basicStream(Attributes.of(Attribute.TITLE, "Michiel de Ruyter", Attribute.YEAR, "2015")));

    assertEquals(0.418, t.b.getMatchAccuracy(), 0.01f);
    assertEquals(MatchType.NAME_AND_RELEASE_DATE, t.b.getMatchType());
    assertEquals(new Identifier(DataSources.TMDB_MOVIE, "80001"), t.a);
  }

  @Test
  public void shouldUseAltTitle() throws JsonProcessingException, IOException {
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("Michiel de Ruyter"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("Admiral, The"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("The Admiral"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("Admiral"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));

    Tuple2<Identifier, Identification> t = service.identify(basicStream(Attributes.of(Attribute.TITLE, "Michiel de Ruyter", Attribute.ALTERNATIVE_TITLE, "Admiral, The", Attribute.YEAR, "2015")));

    assertEquals(1.0, t.b.getMatchAccuracy(), 0.01f);
    assertEquals(MatchType.NAME_AND_RELEASE_DATE, t.b.getMatchType());
    assertEquals(new Identifier(DataSources.TMDB_MOVIE, "80001"), t.a);
  }

  @Test
  public void shouldIdentifyByImdbId() throws JsonProcessingException, IOException {
    when(tmdb.query(eq("3/find/12345"), eq("external_source"), eq("imdb_id"))).thenReturn(objectMapper.readTree("{\"movie_results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));

    Tuple2<Identifier, Identification> t = service.identify(basicStream(Attributes.of(Attribute.TITLE, "Michiel de Ruyter", Attribute.YEAR, "2015", Attribute.ID_PREFIX + "IMDB", "12345")));

    assertEquals(1.0, t.b.getMatchAccuracy(), 0.01f);
    assertEquals(MatchType.ID, t.b.getMatchType());
    assertEquals(new Identifier(DataSources.TMDB_MOVIE, "80001"), t.a);
  }

  @SuppressWarnings("static-method")
  @Test
  public void createVariationsShouldCreateCorrectVariations() {
    assertEquals(Arrays.asList("2012"), TextMatcher.createVariations("2012"));
    assertEquals(Arrays.asList("I, Robot"), TextMatcher.createVariations("I, Robot"));
    assertEquals(Arrays.asList("Terminator, The", "The Terminator", "Terminator"), TextMatcher.createVariations("Terminator, The"));
    assertEquals(Arrays.asList("Michiel de Ruyter"), TextMatcher.createVariations("Michiel de Ruyter"));
    assertEquals(Arrays.asList("Admiral, The", "The Admiral", "Admiral"), TextMatcher.createVariations("Admiral, The"));
  }

  private static BasicStream basicStream(Attributes attributes) {
    return new BasicStream(MediaType.of("MOVIE"), new StringURI(""), new StreamID(1), attributes, Collections.emptyList());
  }
}
