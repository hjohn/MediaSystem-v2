package hs.mediasystem.ext.tmdb.serie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.AbstractIdentificationService;
import hs.mediasystem.ext.basicmediatypes.Attribute;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identification.MatchType;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.SerieStream;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Levenshtein;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TmdbIdentificationService extends AbstractIdentificationService<SerieStream> {
  private static final Logger LOGGER = Logger.getLogger(TmdbIdentificationService.class.getName());
  private static final Pattern TITLES = Pattern.compile("(.*?)(?: \\((.*?)\\))?");  // Matches a normal title and optionally one between parenthesis
  private static final Pattern SEQUENCES = Pattern.compile("0*([1-9][0-9]+)");

  @Inject private TheMovieDatabase tmdb;

  public TmdbIdentificationService() {
    super(DataSources.TMDB_SERIE);
  }

  @Override
  public Identification identify(SerieStream stream) {
    Set<Identifier> knownIdentifiers = stream.getMediaRecords().keySet();
    String imdb = knownIdentifiers.stream().filter(i -> i.getDataSource().equals(DataSources.IMDB_SERIE)).findAny().map(Identifier::getId).orElse(null);
    Identification result = null;

    if(imdb != null) {
      result = identifyByIMDB(imdb);
    }

    if(result == null) {
      result = identifyByStream(stream);
    }

    return result;
  }

  private Identification identifyByIMDB(String imdb) {
    JsonNode node = tmdb.query("3/find/" + imdb, "external_source", "imdb_id");

    return StreamSupport.stream(node.path("tv_results").spliterator(), false)
      .findFirst()
      .map(n -> new Identification(new Identifier(DataSources.TMDB_SERIE, n.get("id").asText()), MatchType.ID, 1))
      .orElse(null);
  }

  private Identification identifyByStream(SerieStream stream) {
    Attributes attributes = stream.getAttributes();
    List<String> titleVariations = createVariations(attributes.get(Attribute.TITLE));

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

    return titleVariations.stream()
      .map(tv -> tv + postFix)
      .peek(q -> LOGGER.fine("Matching '" + q + "' [" + year + "] ..."))
      .flatMap(q -> StreamSupport.stream(tmdb.query("3/search/tv", "query", q, "language", "en").path("results").spliterator(), false)
        .flatMap(jsonNode -> Stream.of(jsonNode.path("name").asText(), jsonNode.path("original_name").asText())
          .filter(t -> !t.isEmpty())
          .distinct()
          .map(t -> createMatch(jsonNode, q.toLowerCase(), year, t))
          .peek(m -> LOGGER.fine("Match for '" + q + "' [" + year + "] -> " + m))
        )
      )
      .max(Comparator.comparingDouble(m -> m.score))
      .map(match -> new Identification(new Identifier(DataSources.TMDB_SERIE, match.movie.get("id").asText()), match.type, match.score / 100))
      .orElse(null);
  }

  private static Match createMatch(JsonNode resultNode, String titleToMatch, Integer year, String nodeTitle) {
    LocalDate releaseDate = TheMovieDatabase.parseDateOrNull(resultNode.path("first_air_date").asText());
    Integer movieYear = extractYear(releaseDate);

    MatchType nameMatchType = MatchType.NAME;
    double score = 0;

    if(year != null && movieYear != null) {
      if(year.equals(movieYear)) {
        nameMatchType = MatchType.NAME_AND_RELEASE_DATE;
        score += 45;
      }
      else if(Math.abs(year - movieYear) == 1) {
        score += 5;
      }
    }

    double matchScore = Levenshtein.compare(nodeTitle.toLowerCase(), titleToMatch);

    score += matchScore * 55;

    return new Match(resultNode, nameMatchType, score);
  }

  static List<String> createVariations(String fullTitle) {
    Matcher matcher = TITLES.matcher(fullTitle);

    if(!matcher.matches()) {
      throw new IllegalStateException("title did not match pattern: " + fullTitle);
    }

    String title = matcher.group(1);
    String secondaryTitle = matcher.group(2);  // Translated title, or alternative title

    List<String> variations = new ArrayList<>();

    variations.add(title);
    variations.addAll(createPronounVariations(title));

    if(secondaryTitle != null) {
      variations.add(secondaryTitle);
      variations.addAll(createPronounVariations(secondaryTitle));
    }

    return variations;
  }

  private static List<String> createPronounVariations(String title) {
    int comma = title.indexOf(", ");

    if(comma < 0) {
      return Collections.emptyList();
    }

    List<String> variations = new ArrayList<>();

    variations.add(title.substring(comma + 2) + " " + title.substring(0, comma));  // With pronoun at start
    variations.add(title.substring(0, comma));  // Without pronoun

    return variations;
  }

  private static Integer extractYear(LocalDate date) {
    if(date == null) {
      return null;
    }

    return date.getYear();
  }

  private static class Match {
    final JsonNode movie;
    final MatchType type;
    final double score;

    Match(JsonNode movie, MatchType matchType, double score) {
      this.movie = movie;
      this.type = matchType;
      this.score = score;
    }

    @Override
    public String toString() {
      String nodeOriginalTitle = movie.path("original_name").asText();
      String name = movie.path("name").asText() + (nodeOriginalTitle != null ? " (" + nodeOriginalTitle + ")" : "");

      return String.format("Match[%6.2f, %s : %s]", score, name, movie.path("first_air_date").asText());
    }
  }
}
