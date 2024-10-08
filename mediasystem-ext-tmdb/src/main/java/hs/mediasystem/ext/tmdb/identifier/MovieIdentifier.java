package hs.mediasystem.ext.tmdb.identifier;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.api.datasource.domain.Identification;
import hs.mediasystem.api.discovery.Attribute;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.TextMatcher;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;
import hs.mediasystem.ext.tmdb.provider.MovieProvider;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.checked.CheckedOptional;
import hs.mediasystem.util.checked.CheckedStreams;

import java.io.IOException;
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
public class MovieIdentifier {
  private static final Logger LOGGER = Logger.getLogger(MovieIdentifier.class.getName());
  private static final Pattern SEQUENCES = Pattern.compile("0*([1-9][0-9]*)");

  @Inject private TheMovieDatabase tmdb;
  @Inject private MovieProvider movieProvider;

  private static record Result(WorkId id, Match match) {}

  public Optional<Identification> identify(Attributes attributes) throws IOException {
    return CheckedOptional.ofNullable((String)attributes.get(Attribute.ID_PREFIX + "IMDB"))
      .flatMap(this::identifyByIMDB)
      .or(() -> identifyByStream(attributes))
      .flatMapOpt(this::toIdentification)
      .toOptional();
  }

  private Optional<Identification> toIdentification(Result result) throws IOException {
    return movieProvider.provide(result.id.getKey())
      .map(movie -> new Identification(List.of(movie), result.match));
  }

  private CheckedOptional<Result> identifyByIMDB(String imdb) throws IOException {
    JsonNode node = tmdb.get("3/find/" + imdb, null, List.of("external_source", "imdb_id"));

    return CheckedOptional.from(StreamSupport.stream(node.path("movie_results").spliterator(), false)
      .findFirst()
      .map(n -> new Result(new WorkId(DataSources.TMDB, MediaType.MOVIE, n.get("id").asText()), new Match(Type.ID, 1, Instant.now())))
    );
  }

  private CheckedOptional<Result> identifyByStream(Attributes attributes) throws IOException {
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

    return CheckedStreams.forIOException(Stream.concat(titleVariations.stream(), alternativeTitleVariations.stream()))
      .map(tv -> tv + postFix)
      .peek(q -> LOGGER.fine("Matching '" + q + "' [" + year + "] ..."))
      .flatMapStream(
        q -> StreamSupport.stream(
          tmdb.get("3/search/movie", null, List.of("query", q, "language", "en", "include_adult", "true"))
            .path("results")
            .spliterator(),
          false
        )
        .flatMap(jsonNode -> Stream.of(jsonNode.path("title").asText(), jsonNode.path("original_title").asText())
          .filter(t -> !t.isEmpty())
          .distinct()
          .map(t -> TextMatcher.createMatch(TheMovieDatabase.parseDateOrNull(jsonNode.path("release_date").asText()), q, year, t, jsonNode.path("id").asText()))
          .peek(m -> LOGGER.fine("Match for '" + q + "' [" + year + "] -> " + m))
        )
      )
      .max(Comparator.comparingDouble(m -> m.getNormalizedScore()))
      .map(match -> new Result(new WorkId(DataSources.TMDB, MediaType.MOVIE, match.getId()), new Match(match.getType(), match.getScore() / 100, Instant.now())));
  }
}
