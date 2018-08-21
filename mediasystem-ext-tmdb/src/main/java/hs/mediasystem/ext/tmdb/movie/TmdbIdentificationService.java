package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identification.MatchType;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.scan.Attribute;
import hs.mediasystem.ext.basicmediatypes.services.AbstractIdentificationService;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.TextMatcher;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;
import hs.mediasystem.util.Attributes;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TmdbIdentificationService extends AbstractIdentificationService {
  private static final Logger LOGGER = Logger.getLogger(TmdbIdentificationService.class.getName());
  private static final Pattern SEQUENCES = Pattern.compile("0*([1-9][0-9]*)");

  @Inject private TheMovieDatabase tmdb;

  public TmdbIdentificationService() {
    super(DataSources.TMDB_MOVIE);
  }

  @Override
  public Identification identify(Attributes attributes) {
    String imdb = attributes.get(Attribute.ID_PREFIX + "IMDB");
    Identification result = null;

    if(imdb != null) {
      result = identifyByIMDB(imdb);
    }

    if(result == null) {
      result = identifyByStream(attributes);
    }

    return result;
  }

  private Identification identifyByIMDB(String imdb) {
    JsonNode node = tmdb.query("3/find/" + imdb, "external_source", "imdb_id");

    return StreamSupport.stream(node.path("movie_results").spliterator(), false)
      .findFirst()
      .map(n -> new Identification(new Identifier(DataSources.TMDB_MOVIE, n.get("id").asText()), MatchType.ID, 1))
      .orElse(null);
  }

  private Identification identifyByStream(Attributes attributes) {
    List<String> titleVariations = TextMatcher.createVariations(attributes.get(Attribute.TITLE));
    List<String> alternativeTitleVariations = TextMatcher.createVariations(attributes.get(Attribute.ALTERNATIVE_TITLE));

    String subtitle = attributes.get(Attribute.SUBTITLE, "");
    Integer year = attributes.asInteger(Attribute.YEAR);
    String seq = null;

    if(attributes.contains(Attribute.SEQUENCE)) {
      Matcher matcher = SEQUENCES.matcher(attributes.get(Attribute.SEQUENCE));

      if(matcher.matches()) {
        seq = matcher.group(1);
      }
    }

    String postFix = (seq != null && !seq.equals("1") ? " " + seq : "") + (!subtitle.isEmpty() ? " " + subtitle : "");

    return Stream.concat(titleVariations.stream(), alternativeTitleVariations.stream())
      .map(tv -> tv + postFix)
      .peek(q -> LOGGER.fine("Matching '" + q + "' [" + year + "] ..."))
      .flatMap(q -> StreamSupport.stream(tmdb.query("3/search/movie", "query", q, "language", "en").path("results").spliterator(), false)
        .flatMap(jsonNode -> Stream.of(jsonNode.path("title").asText(), jsonNode.path("original_title").asText())
          .filter(t -> !t.isEmpty())
          .distinct()
          .map(t -> TextMatcher.createMatch(TheMovieDatabase.parseDateOrNull(jsonNode.path("release_date").asText()), q, year, t, jsonNode.path("id").asText()))
          .peek(m -> LOGGER.fine("Match for '" + q + "' [" + year + "] -> " + m))
        )
      )
      .max(Comparator.comparingDouble(m -> m.getScore()))
      .map(match -> new Identification(new Identifier(DataSources.TMDB_MOVIE, match.getId()), match.getType(), match.getScore() / 100))
      .orElse(null);
  }
}
