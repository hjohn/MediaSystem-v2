package hs.mediasystem.ext.tmdb.serie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.ddif.annotations.PluginScoped;
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
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.checked.Flow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.inject.Inject;

@PluginScoped
public class TmdbQueryService extends AbstractQueryService {
  private static final Logger LOGGER = Logger.getLogger(TmdbQueryService.class.getName());
  private static final int MAX_SEASONS_PER_QUERY = 10;

  @Inject private TheMovieDatabase tmdb;
  @Inject private PersonRoles personRoles;
  @Inject private ObjectFactory objectFactory;

  public TmdbQueryService() {
    super(DataSources.TMDB_SERIE);
  }

  @Override
  public Serie query(Identifier identifier) throws IOException {
    JsonNode node = tmdb.query("3/tv/" + identifier.getId(), "text:json:" + identifier, List.of("append_to_response", "keywords,content_ratings"));  // credits,videos,keywords,alternative_titles,recommendations,similar,reviews
    List<JsonNode> seasons = batchQuerySeasons(identifier, node);

    // Popularity... Status... last air date ... inproduction field
    //['Returning Series', 'Planned', 'In Production', 'Ended', 'Canceled', 'Pilot']

    return objectFactory.toSerie(node, Flow.forIOException(seasons.stream()).map(s -> toSeason(s, identifier.getId())).collect(Collectors.toList()));
  }

  private List<JsonNode> batchQuerySeasons(Identifier identifier, JsonNode node) throws IOException {
    List<JsonNode> seasons = new ArrayList<>();
    JsonNode seasonsPath = node.path("seasons");
    String firstSeasonNumber = "";
    String appendToResponse = "";

    for(int i = 1; i <= seasonsPath.size(); i++) {
      JsonNode season = seasonsPath.get(i - 1);
      String seasonNumber = season.get("season_number").asText();

      if(!appendToResponse.isEmpty()) {
        firstSeasonNumber = seasonNumber;
        appendToResponse += ",";
      }

      appendToResponse += "season/" + seasonNumber;

      if(i % MAX_SEASONS_PER_QUERY == 0 || i == seasonsPath.size()) {
        JsonNode seasonData = tmdb.query(
          "3/tv/" + identifier.getId(),
          "text:json:" + new ProductionIdentifier(DataSources.TMDB_SEASON, identifier.getId() + "/" + firstSeasonNumber + "-" + seasonNumber),
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

    ProductionIdentifier identifier = new ProductionIdentifier(DataSources.TMDB_SEASON, parentId + "/" + seasonNumber);

    return new Season(
      identifier,
      new Details(
        Optional.ofNullable(node.get("name")).map(JsonNode::textValue).orElse("(untitled)"),
        null,
        node.path("overview").textValue(),
        TheMovieDatabase.parseDateOrNull(releaseDate),
        tmdb.createImageURI(node.path("poster_path").textValue(), "original", "image:cover:" + identifier),  // as cover
        null,
        null
      ),
      seasonNumber,
      episodes
    );
  }

  private Episode toEpisode(JsonNode node, String parentId) throws IOException {
    String releaseDate = node.path("air_date").textValue();
    Reception reception = node.get("vote_count").isNumber() && node.get("vote_average").isNumber() ?
      new Reception(node.get("vote_average").asDouble(), node.get("vote_count").asInt()) : null;
    int seasonNumber = node.get("season_number").asInt();
    int episodeNumber = node.get("episode_number").asInt();
    EpisodeIdentifier identifier = new EpisodeIdentifier(DataSources.TMDB_EPISODE, parentId + "/" + seasonNumber + "/" + episodeNumber);

    return new Episode(
      identifier,
      new Details(
        Optional.ofNullable(node.get("name")).map(JsonNode::textValue).orElse("(untitled)"),
        null,
        node.path("overview").textValue(),
        TheMovieDatabase.parseDateOrNull(releaseDate),
        null,
        tmdb.createImageURI(node.path("still_path").textValue(), "original", "image:cover:" + identifier),  // as sample image
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
