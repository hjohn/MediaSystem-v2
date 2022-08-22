package hs.mediasystem.ext.tmdb.serie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.PersonRoles;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.checked.CheckedStreams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TmdbQueryService extends AbstractQueryService {
  private static final Logger LOGGER = Logger.getLogger(TmdbQueryService.class.getName());
  private static final int MAX_SEASONS_PER_QUERY = 10;

  @Inject private TheMovieDatabase tmdb;
  @Inject private PersonRoles personRoles;
  @Inject private ObjectFactory objectFactory;

  public TmdbQueryService() {
    super(DataSources.TMDB, MediaType.SERIE);
  }

  @Override
  public Serie query(WorkId id) throws IOException {
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
        firstSeasonNumber = seasonNumber;
        appendToResponse += ",";
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
