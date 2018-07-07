package hs.mediasystem.ext.tmdb.serie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.AbstractQueryService;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.PersonRoles;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;
import hs.mediasystem.util.ImageURI;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class TmdbQueryService extends AbstractQueryService<Serie> {
  @Inject private TheMovieDatabase tmdb;
  @Inject private PersonRoles personRoles;

  public TmdbQueryService() {
    super(DataSources.TMDB_SERIE);
  }

  @Override
  public Result<Serie> query(Identifier identifier) {
    JsonNode node = tmdb.query("3/tv/" + identifier.getId(), "append_to_response", "videos");  // credits,videos,keywords,alternative_titles,recommendations,similar,reviews
    List<JsonNode> seasons = new ArrayList<>();

    for(JsonNode season : node.path("seasons")) {
      seasons.add(tmdb.query("3/tv/" + identifier.getId() + "/season/" + season.get("season_number").asText(), "append_to_response", "videos"));
    }

    String releaseDate = node.path("first_air_date").textValue();
    Number runtime = node.path("episode_run_time").numberValue();

    ImageURI backdropURI = tmdb.createImageURI(node.path("backdrop_path").textValue(), "original");
    ImageURI posterURI = tmdb.createImageURI(node.path("poster_path").textValue(), "original");

    Reception reception = node.get("vote_count").isNumber() && node.get("vote_average").isNumber() ?
      new Reception(node.get("vote_average").asDouble(), node.get("vote_count").asInt()) : null;

    Serie descriptor = new Serie(
      new Production(
        new ProductionIdentifier(DataSources.TMDB_SERIE, identifier.getId()),
        node.get("name").textValue(),
        node.path("overview").textValue(),
        releaseDate == null ? null : LocalDate.parse(releaseDate, DateTimeFormatter.ISO_DATE),
        posterURI,
        backdropURI,
        reception,
        runtime == null ? null : Duration.ofMinutes(runtime.intValue()),
        node.path("languages").findValuesAsText("name"),
        node.path("genres").findValuesAsText("name")
      ),
      seasons.stream().map(s -> toSeason(s, identifier.getId())).collect(Collectors.toList())
    );

    return Result.of(descriptor, Collections.emptySet());
  }

  private Season toSeason(JsonNode node, String parentId) {
    List<JsonNode> episodes = new ArrayList<>();

    for(JsonNode episode : node.at("/episodes")) {
      episodes.add(episode);
    }

    String releaseDate = node.path("air_date").textValue();
    int seasonNumber = node.get("season_number").asInt();

    return new Season(
      seasonNumber,
      new Production(
        new ProductionIdentifier(DataSources.TMDB_SEASON, parentId + "/" + seasonNumber),
        node.get("name").asText(),
        node.get("overview").asText(),
        releaseDate == null ? null : LocalDate.parse(releaseDate, DateTimeFormatter.ISO_DATE),
        tmdb.createImageURI(node.path("poster_path").textValue(), "original"),
        null,
        null,
        null,
        Collections.emptyList(),
        Collections.emptyList()
      ),
      episodes.stream().map(e -> toEpisode(e, parentId)).collect(Collectors.toList())
    );
  }

  private Episode toEpisode(JsonNode node, String parentId) {
    String releaseDate = node.path("air_date").textValue();
    Reception reception = node.get("vote_count").isNumber() && node.get("vote_average").isNumber() ?
      new Reception(node.get("vote_average").asDouble(), node.get("vote_count").asInt()) : null;
      int seasonNumber = node.get("season_number").asInt();
    int episodeNumber = node.get("episode_number").asInt();

    return new Episode(
      seasonNumber,
      episodeNumber,
      new Production(
        new ProductionIdentifier(DataSources.TMDB_EPISODE, parentId + "/" + seasonNumber + "/" + episodeNumber),
        node.get("name").asText(),
        node.get("overview").asText(),
        releaseDate == null ? null : LocalDate.parse(releaseDate, DateTimeFormatter.ISO_DATE),
        tmdb.createImageURI(node.path("still_path").textValue(), "original"),
        null,
        reception,
        null,
        Collections.emptyList(),
        Collections.emptyList()
      ),
      personRoles.toPersonRoles(node)
    );
  }
}
