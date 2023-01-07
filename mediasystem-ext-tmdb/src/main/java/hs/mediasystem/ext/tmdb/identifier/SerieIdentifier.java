package hs.mediasystem.ext.tmdb.identifier;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService.Identification;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.PersonRoles;
import hs.mediasystem.ext.tmdb.TextMatcher;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.checked.CheckedOptional;
import hs.mediasystem.util.checked.CheckedStreams;
import hs.mediasystem.util.exception.Throwables;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SerieIdentifier {
  private static final Logger LOGGER = Logger.getLogger(SerieIdentifier.class.getName());
  private static final Pattern SEQUENCES = Pattern.compile("0*([1-9][0-9]+)");
  private static final int MAX_SEASONS_PER_QUERY = 10;

  @Inject private TheMovieDatabase tmdb;
  @Inject private ObjectFactory objectFactory;
  @Inject private PersonRoles personRoles;

  private static record Result(WorkId id, Match match) {}

  public Optional<Identification> identify(Attributes attributes) throws IOException {
    return CheckedOptional.ofNullable((String)attributes.get(Attribute.ID_PREFIX + "IMDB"))
      .flatMap(this::identifyByIMDB)
      .or(() -> identifyByStream(attributes))
      .map(r -> new Identification(List.of(query(r.id)), r.match()))
      .toOptional();
  }

  private CheckedOptional<Result> identifyByIMDB(String imdb) throws IOException {
    JsonNode node = tmdb.query("3/find/" + imdb, null, List.of("external_source", "imdb_id"));

    return CheckedOptional.from(StreamSupport.stream(node.path("tv_results").spliterator(), false)
      .findFirst()
      .map(n -> new Result(new WorkId(DataSources.TMDB, MediaType.SERIE, n.get("id").asText()), new Match(Type.ID, 1, Instant.now())))
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
      .flatMapStream(q -> StreamSupport.stream(tmdb.query("3/search/tv", null, List.of("query", q, "language", "en", "include_adult", "true")).path("results").spliterator(), false)
        .flatMap(jsonNode -> Stream.of(jsonNode.path("name").asText(), jsonNode.path("original_name").asText())
          .filter(t -> !t.isEmpty())
          .distinct()
          .map(t -> TextMatcher.createMatch(TheMovieDatabase.parseDateOrNull(jsonNode.path("first_air_date").asText()), q, year, t, jsonNode.path("id").asText()))
          .peek(m -> LOGGER.fine("Match for '" + q + "' [" + year + "] -> " + m))
        )
      )
      .max(Comparator.comparingDouble(m -> m.getNormalizedScore()))
      .map(match -> new Result(new WorkId(DataSources.TMDB, MediaType.SERIE, match.getId()), new Match(match.getType(), match.getScore() / 100, Instant.now())));
  }

  private Serie query(WorkId id) throws IOException {
    JsonNode node = tmdb.query("3/tv/" + id.getKey(), "text:json:" + id, List.of("append_to_response", "keywords,content_ratings"));  // credits,videos,keywords,alternative_titles,recommendations,similar,reviews
    List<JsonNode> seasons = batchQuerySeasons(id, node);

    // Popularity... Status... last air date ... inproduction field
    //['Returning Series', 'Planned', 'In Production', 'Ended', 'Canceled', 'Pilot']

    return objectFactory.toSerie(node, CheckedStreams.forIOException(seasons.stream()).map(s -> toSeason(s, id.getKey())).collect(Collectors.toList()));
  }

  private List<JsonNode> batchQuerySeasons(WorkId id, JsonNode node) throws IOException {
    List<JsonNode> seasons = new ArrayList<>();
    JsonNode seasonsPath = node.path("seasons");
    String firstSeasonNumber = "";
    String appendToResponse = "";

    for(int i = 1; i <= seasonsPath.size(); i++) {
      JsonNode season = seasonsPath.get(i - 1);
      String seasonNumber = season.get("season_number").asText();

      if(!appendToResponse.isEmpty()) {
        appendToResponse += ",";
      }
      else {
        firstSeasonNumber = seasonNumber;
      }

      appendToResponse += "season/" + seasonNumber;

      if(i % MAX_SEASONS_PER_QUERY == 0 || i == seasonsPath.size()) {
        JsonNode seasonData = tmdb.query(
          "3/tv/" + id.getKey(),
          "text:json:" + new WorkId(DataSources.TMDB, MediaType.SEASON, id.getKey() + "/" + firstSeasonNumber + "-" + seasonNumber),
          List.of("append_to_response", appendToResponse)
        );

        for(String path : appendToResponse.split(",")) {
          JsonNode seasonPath = seasonData.path(path);

          if(seasonPath.isContainerNode()) {
            seasons.add(seasonPath);
          }
        }

        appendToResponse = "";
      }
    }

    return seasons;
  }

  private Season toSeason(JsonNode node, String parentId) throws IOException {
    List<Episode> episodes = new ArrayList<>();

    for(JsonNode episode : node.at("/episodes")) {
      try {
        episodes.add(toEpisode(episode, parentId));
      }
      catch(RuntimeException e) {
        LOGGER.warning("Skipping Episode entry, exception while parsing: " + episode + ": " + Throwables.formatAsOneLine(e));
      }
    }

    String releaseDate = node.path("air_date").textValue();
    int seasonNumber = node.get("season_number").asInt();

    WorkId id = new WorkId(DataSources.TMDB, MediaType.SEASON, parentId + "/" + seasonNumber);

    return new Season(
      id,
      new Details(
        Optional.ofNullable(node.get("name")).map(JsonNode::textValue).orElse("(untitled)"),
        null,
        node.path("overview").textValue(),
        TheMovieDatabase.parseDateOrNull(releaseDate),
        tmdb.createImageURI(node.path("poster_path").textValue(), "original", "image:cover:" + id),  // as cover
        null,
        null
      ),
      seasonNumber,
      episodes
    );
  }

  private Episode toEpisode(JsonNode node, String parentKey) throws IOException {
    String releaseDate = node.path("air_date").textValue();
    Reception reception = node.get("vote_count").isNumber() && node.get("vote_average").isNumber() ?
      new Reception(node.get("vote_average").asDouble(), node.get("vote_count").asInt()) : null;
    int seasonNumber = node.get("season_number").asInt();
    int episodeNumber = node.get("episode_number").asInt();
    WorkId id = new WorkId(DataSources.TMDB, MediaType.EPISODE, parentKey + "/" + seasonNumber + "/" + episodeNumber);

    return new Episode(
      id,
      new Details(
        Optional.ofNullable(node.get("name")).map(JsonNode::textValue).orElse("(untitled)"),
        null,
        node.path("overview").textValue(),
        TheMovieDatabase.parseDateOrNull(releaseDate),
        null,
        tmdb.createImageURI(node.path("still_path").textValue(), "original", "image:cover:" + id),  // as sample image
        null
      ),
      reception,
      null,
      seasonNumber,
      episodeNumber,
      personRoles.toPersonRoles(node)
    );
  }
}
