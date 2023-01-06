package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import hs.ddif.annotations.Produces;
import hs.mediasystem.db.InjectorExtension;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.ext.basicmediatypes.api.Discovery;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrint;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService.Identification;
import hs.mediasystem.util.Attributes;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(InjectorExtension.class)
public class TmdbIdentificationServiceTest {
  @Produces private static final TheMovieDatabase TMDB = mock(TheMovieDatabase.class); // rename
  @Produces private static final ObjectFactory OBJECT_FACTORY = mock(ObjectFactory.class);

  private ObjectMapper objectMapper = new ObjectMapper();

  @Inject private TmdbIdentificationService service;

  private Movie movie = mock(Movie.class);

  @BeforeEach
  void beforeEach() throws IOException {
    when(OBJECT_FACTORY.toMovie(any())).thenReturn(movie);
  }

  @Test
  public void shouldIdentifyMovies() throws JsonProcessingException, IOException {
    when(TMDB.query(eq("3/search/movie"), eq(null), eq(List.of("query", "Terminator 5 Genisys", "language", "en", "include_adult", "true")))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80000,\"original_title\":\"Terminator Genisys\",\"release_date\":\"2015-07-01\",\"title\":\"Terminator Genisys\"}]}"));
    when(TMDB.query(eq("3/search/movie"), eq(null), eq(List.of("query", "Terminator, The 5 Genisys", "language", "en", "include_adult", "true")))).thenReturn(objectMapper.readTree("{\"results\":[]}"));
    when(TMDB.query(eq("3/search/movie"), eq(null), eq(List.of("query", "The Terminator 5 Genisys", "language", "en", "include_adult", "true")))).thenReturn(objectMapper.readTree("{\"results\":[]}"));

    Identification identification = service.identify(discovery(Attributes.of(Attribute.TITLE, "Terminator, The", Attribute.YEAR, "2015", Attribute.SUBTITLE, "Genisys", Attribute.SEQUENCE, "5")), null).get();

    assertEquals(0.77, identification.match().accuracy(), 0.01f);
    assertEquals(Type.NAME_AND_RELEASE_DATE, identification.match().type());
  }

  @Test
  public void shouldIdentifyMovies2() throws JsonProcessingException, IOException {
    when(TMDB.query(eq("3/search/movie"), eq(null), eq(List.of("query", "Michiel de Ruyter", "language", "en", "include_adult", "true")))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"original_title\":\"Michiel de Ruyter\",\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));

    Identification identification = service.identify(discovery(Attributes.of(Attribute.TITLE, "Michiel de Ruyter", Attribute.YEAR, "2015")), null).get();

    assertEquals(1.0, identification.match().accuracy(), 0.01f);
    assertEquals(Type.NAME_AND_RELEASE_DATE, identification.match().type());
  }

  @Test
  public void shouldNotFailWhenNoOriginalTitlePresent() throws JsonProcessingException, IOException {
    when(TMDB.query(eq("3/search/movie"), eq(null), eq(List.of("query", "Michiel de Ruyter", "language", "en", "include_adult", "true")))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));

    Identification identification = service.identify(discovery(Attributes.of(Attribute.TITLE, "Michiel de Ruyter", Attribute.YEAR, "2015")), null).get();

    assertEquals(0.470, identification.match().accuracy(), 0.01f);
    assertEquals(Type.NAME_AND_RELEASE_DATE, identification.match().type());
  }

  @Test
  public void shouldUseAltTitle() throws JsonProcessingException, IOException {
    when(TMDB.query(eq("3/search/movie"), eq(null), eq(List.of("query", "Michiel de Ruyter", "language", "en", "include_adult", "true")))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));
    when(TMDB.query(eq("3/search/movie"), eq(null), eq(List.of("query", "Admiral, The", "language", "en", "include_adult", "true")))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));
    when(TMDB.query(eq("3/search/movie"), eq(null), eq(List.of("query", "The Admiral", "language", "en", "include_adult", "true")))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));
    when(TMDB.query(eq("3/search/movie"), eq(null), eq(List.of("query", "Admiral", "language", "en", "include_adult", "true")))).thenReturn(objectMapper.readTree("{\"results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));

    Identification identification = service.identify(discovery(Attributes.of(Attribute.TITLE, "Michiel de Ruyter", Attribute.ALTERNATIVE_TITLE, "Admiral, The", Attribute.YEAR, "2015")), null).get();

    assertEquals(1.0, identification.match().accuracy(), 0.01f);
    assertEquals(Type.NAME_AND_RELEASE_DATE, identification.match().type());
  }

  @Test
  public void shouldIdentifyByImdbId() throws JsonProcessingException, IOException {
    when(TMDB.query(eq("3/find/12345"), eq(null), eq(List.of("external_source", "imdb_id")))).thenReturn(objectMapper.readTree("{\"movie_results\":[{\"id\":80001,\"release_date\":\"2015-07-01\",\"title\":\"Admiral\"}]}"));

    Identification identification = service.identify(discovery(Attributes.of(Attribute.TITLE, "Michiel de Ruyter", Attribute.YEAR, "2015", Attribute.ID_PREFIX + "IMDB", "12345")), null).get();

    assertEquals(1.0, identification.match().accuracy(), 0.01f);
    assertEquals(Type.ID, identification.match().type());
  }

  @Test
  public void createVariationsShouldCreateCorrectVariations() {
    assertEquals(Arrays.asList("2012"), TextMatcher.createVariations("2012"));
    assertEquals(Arrays.asList("I, Robot"), TextMatcher.createVariations("I, Robot"));
    assertEquals(Arrays.asList("Terminator, The", "The Terminator", "Terminator"), TextMatcher.createVariations("Terminator, The"));
    assertEquals(Arrays.asList("Michiel de Ruyter"), TextMatcher.createVariations("Michiel de Ruyter"));
    assertEquals(Arrays.asList("Admiral, The", "The Admiral", "Admiral"), TextMatcher.createVariations("Admiral, The"));
  }

  private static Discovery discovery(Attributes attributes) {
    return new Discovery(MediaType.MOVIE, URI.create(""), attributes, Optional.empty(), new ContentPrint(new ContentID(1), 12345L, 123456L, new byte[] {1, 2, 3}, Instant.now()));
  }
}
