package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.tmdb.movie.TmdbIdentificationService;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

import java.io.IOException;
import java.util.Arrays;

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

    Identification identification = service.identify(streamable(Attributes.of(Attribute.TITLE, "Terminator, The", Attribute.YEAR, "2015", Attribute.SUBTITLE, "Genisys", Attribute.SEQUENCE, "5")), null).get();

    assertEquals(0.77, identification.getMatch().getAccuracy(), 0.01f);
    assertEquals(Type.NAME_AND_RELEASE_DATE, identification.getMatch().getType());
    assertEquals(new Identifier(DataSources.TMDB_MOVIE, "80000"), identification.getPrimaryIdentifier());
  }

  @Test
  public void shouldIdentifyMovies2() throws JsonProcessingException, IOException {
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("Michiel de Ruyter"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"original_title\":\"Michiel de Ruyter\",\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));

    Identification identification = service.identify(streamable(Attributes.of(Attribute.TITLE, "Michiel de Ruyter", Attribute.YEAR, "2015")), null).get();

    assertEquals(1.0, identification.getMatch().getAccuracy(), 0.01f);
    assertEquals(Type.NAME_AND_RELEASE_DATE, identification.getMatch().getType());
    assertEquals(new Identifier(DataSources.TMDB_MOVIE, "80001"), identification.getPrimaryIdentifier());
  }

  @Test
  public void shouldNotFailWhenNoOriginalTitlePresent() throws JsonProcessingException, IOException {
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("Michiel de Ruyter"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));

    Identification identification = service.identify(streamable(Attributes.of(Attribute.TITLE, "Michiel de Ruyter", Attribute.YEAR, "2015")), null).get();

    assertEquals(0.470, identification.getMatch().getAccuracy(), 0.01f);
    assertEquals(Type.NAME_AND_RELEASE_DATE, identification.getMatch().getType());
    assertEquals(new Identifier(DataSources.TMDB_MOVIE, "80001"), identification.getPrimaryIdentifier());
  }

  @Test
  public void shouldUseAltTitle() throws JsonProcessingException, IOException {
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("Michiel de Ruyter"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("Admiral, The"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("The Admiral"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));
    when(tmdb.query(eq("3/search/movie"), eq("query"), eq("Admiral"), eq("language"), eq("en"))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));

    Identification identification = service.identify(streamable(Attributes.of(Attribute.TITLE, "Michiel de Ruyter", Attribute.ALTERNATIVE_TITLE, "Admiral, The", Attribute.YEAR, "2015")), null).get();

    assertEquals(1.0, identification.getMatch().getAccuracy(), 0.01f);
    assertEquals(Type.NAME_AND_RELEASE_DATE, identification.getMatch().getType());
    assertEquals(new Identifier(DataSources.TMDB_MOVIE, "80001"), identification.getPrimaryIdentifier());
  }

  @Test
  public void shouldIdentifyByImdbId() throws JsonProcessingException, IOException {
    when(tmdb.query(eq("3/find/12345"), eq("external_source"), eq("imdb_id"))).thenReturn(objectMapper.readTree("{\"movie_results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));

    Identification identification = service.identify(streamable(Attributes.of(Attribute.TITLE, "Michiel de Ruyter", Attribute.YEAR, "2015", Attribute.ID_PREFIX + "IMDB", "12345")), null).get();

    assertEquals(1.0, identification.getMatch().getAccuracy(), 0.01f);
    assertEquals(Type.ID, identification.getMatch().getType());
    assertEquals(new Identifier(DataSources.TMDB_MOVIE, "80001"), identification.getPrimaryIdentifier());
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

  private static Streamable streamable(Attributes attributes) {
    return new Streamable(MediaType.of("MOVIE"), new StringURI(""), new ContentID(1), null, attributes);
  }
}
