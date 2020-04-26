package hs.mediasystem.ext.tmdb.serie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.services.AbstractIdentificationService;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.TextMatcher;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;
import hs.mediasystem.util.Attributes;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
  private static final Pattern SEQUENCES = Pattern.compile("0*([1-9][0-9]+)");

  @Inject private TheMovieDatabase tmdb;

  public TmdbIdentificationService() {
    super(DataSources.TMDB_SERIE);
  }

  @Override
  public Optional<Identification> identify(Streamable streamable, MediaDescriptor parent) {
    Attributes attributes = streamable.getAttributes();

    return Optional.ofNullable((String)attributes.get(Attribute.ID_PREFIX + "IMDB"))
      .flatMap(this::identifyByIMDB)
      .or(() -> identifyByStream(attributes));
  }

  private Optional<Identification> identifyByIMDB(String imdb) {
    JsonNode node = tmdb.query("3/find/" + imdb, null, List.of("external_source", "imdb_id"));

    return StreamSupport.stream(node.path("tv_results").spliterator(), false)
      .findFirst()
      .map(n -> new Identification(List.of(new Identifier(DataSources.TMDB_SERIE, n.get("id").asText())), new Match(Type.ID, 1, Instant.now())));
  }

  private Optional<Identification> identifyByStream(Attributes attributes) {
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
      .flatMap(q -> StreamSupport.stream(tmdb.query("3/search/tv", null, List.of("query", q, "language", "en")).path("results").spliterator(), false)
        .flatMap(jsonNode -> Stream.of(jsonNode.path("name").asText(), jsonNode.path("original_name").asText())
          .filter(t -> !t.isEmpty())
          .distinct()
          .map(t -> TextMatcher.createMatch(TheMovieDatabase.parseDateOrNull(jsonNode.path("first_air_date").asText()), q, year, t, jsonNode.path("id").asText()))
          .peek(m -> LOGGER.fine("Match for '" + q + "' [" + year + "] -> " + m))
        )
      )
      .max(Comparator.comparingDouble(m -> m.getNormalizedScore()))
      .map(match -> new Identification(List.of(new Identifier(DataSources.TMDB_SERIE, match.getId())), new Match(match.getType(), match.getScore() / 100, Instant.now())));
  }
}
