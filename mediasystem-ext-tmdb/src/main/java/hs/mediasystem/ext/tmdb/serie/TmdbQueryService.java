package hs.mediasystem.ext.tmdb.serie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.EpisodeIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.PersonRoles;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TmdbQueryService extends AbstractQueryService {
  @Inject private TheMovieDatabase tmdb;
  @Inject private PersonRoles personRoles;
  @Inject private ObjectFactory objectFactory;

  public TmdbQueryService() {
    super(DataSources.TMDB_SERIE);
  }

  @Override
  public Serie query(Identifier identifier) {
    JsonNode node = tmdb.query("3/tv/" + identifier.getId(), "append_to_response", "keywords");  // credits,videos,keywords,alternative_titles,recommendations,similar,reviews
    List<JsonNode> seasons = new ArrayList<>();

    for(JsonNode season : node.path("seasons")) {
      seasons.add(tmdb.query("3/tv/" + identifier.getId() + "/season/" + season.get("season_number").asText()));
    }

    // Popularity... Status... last air date ... inproduction field
    //['Returning Series', 'Planned', 'In Production', 'Ended', 'Canceled', 'Pilot']

    return objectFactory.toSerie(node, seasons.stream().map(s -> toSeason(s, identifier.getId())).collect(Collectors.toList()));
  }

  private Season toSeason(JsonNode node, String parentId) {
    List<JsonNode> episodes = new ArrayList<>();

    for(JsonNode episode : node.at("/episodes")) {
      episodes.add(episode);
    }

    String releaseDate = node.path("air_date").textValue();
    int seasonNumber = node.get("season_number").asInt();

    return new Season(
      new ProductionIdentifier(DataSources.TMDB_SEASON, parentId + "/" + seasonNumber),
      new Details(
        node.get("name").asText(),
        node.get("overview").asText(),
        releaseDate == null || releaseDate.isEmpty() ? null : LocalDate.parse(releaseDate, DateTimeFormatter.ISO_DATE),
        tmdb.createImageURI(node.path("poster_path").textValue(), "original"),
        null
      ),
      seasonNumber,
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
      new EpisodeIdentifier(DataSources.TMDB_EPISODE, parentId + "/" + seasonNumber + "/" + episodeNumber),
      new Details(
        node.get("name").asText(),
        node.get("overview").asText(),
        releaseDate == null ? null : LocalDate.parse(releaseDate, DateTimeFormatter.ISO_DATE),
        tmdb.createImageURI(node.path("still_path").textValue(), "original"),
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
