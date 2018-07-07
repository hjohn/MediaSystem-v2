package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import hs.mediasystem.ext.basicmediatypes.Attribute;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identification.MatchType;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MovieStream;
import hs.mediasystem.ext.basicmediatypes.StreamPrint;
import hs.mediasystem.ext.tmdb.movie.TmdbIdentificationService;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Maps;
import hs.mediasystem.util.StringURI;

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
  private static final StreamPrint STREAM_PRINT = new StreamPrint(new StringURI("file://test"), 1024L, 0L, new byte[] {1, 2, 3}, 12345L);

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
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("Terminator, The 5 Genisys"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{}"));
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("The Terminator 5 Genisys"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{}"));

    Identification i = service.identify(new MovieStream(STREAM_PRINT, Attributes.of(Attribute.TITLE, "Terminator, The", Attribute.YEAR, "2015", Attribute.SUBTITLE, "Genisys", Attribute.SEQUENCE, "5"), Collections.emptyMap()));

    assertEquals(0.945, i.getMatchAccuracy(), 0.01f);
    assertEquals(MatchType.NAME_AND_RELEASE_DATE, i.getMatchType());
    assertEquals(new Identifier(DataSources.TMDB_MOVIE, "80000"), i.getIdentifier());
  }

  @Test
  public void shouldIdentifyMovies2() throws JsonProcessingException, IOException {
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("Michiel de Ruyter"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"original_title\":\"Michiel de Ruyter\",\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));

    Identification i = service.identify(new MovieStream(STREAM_PRINT, Attributes.of(Attribute.TITLE, "Michiel de Ruyter", Attribute.YEAR, "2015"), Collections.emptyMap()));

    assertEquals(1.0, i.getMatchAccuracy(), 0.01f);
    assertEquals(MatchType.NAME_AND_RELEASE_DATE, i.getMatchType());
    assertEquals(new Identifier(DataSources.TMDB_MOVIE, "80001"), i.getIdentifier());
  }

  @Test
  public void shouldNotFailWhenNoOriginalTitlePresent() throws JsonProcessingException, IOException {
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("Michiel de Ruyter"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));

    Identification i = service.identify(new MovieStream(STREAM_PRINT, Attributes.of(Attribute.TITLE, "Michiel de Ruyter", Attribute.YEAR, "2015"), Collections.emptyMap()));

    assertEquals(0.515, i.getMatchAccuracy(), 0.01f);
    assertEquals(MatchType.NAME_AND_RELEASE_DATE, i.getMatchType());
    assertEquals(new Identifier(DataSources.TMDB_MOVIE, "80001"), i.getIdentifier());
  }

  @Test
  public void shouldIdentifyByImdbId() throws JsonProcessingException, IOException {
    when(tmdb.query(eq("3/find/12345"), eq("external_source"), eq("imdb_id"))).thenReturn(objectMapper.readTree("{\"movie_results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));

    Identification i = service.identify(new MovieStream(STREAM_PRINT, Attributes.of(Attribute.TITLE, "Michiel de Ruyter", Attribute.YEAR, "2015"), Maps.create(new Identifier(DataSources.IMDB_MOVIE, "12345"), null)));

    assertEquals(1.0, i.getMatchAccuracy(), 0.01f);
    assertEquals(MatchType.ID, i.getMatchType());
    assertEquals(new Identifier(DataSources.TMDB_MOVIE, "80001"), i.getIdentifier());
  }

  @SuppressWarnings("static-method")
  @Test
  public void createVariationsShouldCreateCorrectVariations() {
    assertEquals(Arrays.asList("2012"), TmdbIdentificationService.createVariations("2012"));
    assertEquals(Arrays.asList("I, Robot", "Robot I", "I"), TmdbIdentificationService.createVariations("I, Robot"));
    assertEquals(Arrays.asList("Terminator, The", "The Terminator", "Terminator"), TmdbIdentificationService.createVariations("Terminator, The"));
    assertEquals(Arrays.asList("Michiel de Ruyter", "Admiral, The", "The Admiral", "Admiral"), TmdbIdentificationService.createVariations("Michiel de Ruyter (Admiral, The)"));
  }
}
