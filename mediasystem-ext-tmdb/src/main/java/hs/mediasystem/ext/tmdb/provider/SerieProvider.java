package hs.mediasystem.ext.tmdb.provider;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.api.datasource.domain.Details;
import hs.mediasystem.api.datasource.domain.Serie;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.PersonRoles;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;
import hs.mediasystem.util.checked.CheckedStreams;
import hs.mediasystem.util.exception.Throwables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SerieProvider implements MediaProvider<Serie> {
  private static final Logger LOGGER = Logger.getLogger(SerieProvider.class.getName());
  private static final int MAX_SEASONS_PER_QUERY = 10;

  @Inject private TheMovieDatabase tmdb;
  @Inject private PersonRoles personRoles;
  @Inject private ObjectFactory objectFactory;

  @Override
  public Optional<Serie> provide(String key) throws IOException {
    JsonNode node = tmdb.query("3/tv/" + key, "text:json:tmdb:serie:" + key, List.of("append_to_response", "keywords,content_ratings"))  // credits,videos,keywords,alternative_titles,recommendations,similar,reviews
      .orElse(null);

    if(node == null) {
      return Optional.empty();
    }

    List<JsonNode> seasons = batchQuerySeasons(key, node);

    // Popularity... Status... last air date ... inproduction field
    //['Returning Series', 'Planned', 'In Production', 'Ended', 'Canceled', 'Pilot']

    return Optional.of(objectFactory.toSerie(node, CheckedStreams.forIOException(seasons.stream()).map(s -> toSeason(s, key)).collect(Collectors.toList())));
  }

  private List<JsonNode> batchQuerySeasons(String key, JsonNode node) throws IOException {
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
        String finalResponse = appendToResponse;

        tmdb
          .query(
            "3/tv/" + key,
            "text:json:tmdb:season:" + key + "/" + firstSeasonNumber + "-" + seasonNumber,
            List.of("append_to_response", finalResponse)
          )
          .ifPresent(seasonData -> {
            for(String path : finalResponse.split(",")) {
              JsonNode seasonPath = seasonData.path(path);

              if(seasonPath.isContainerNode()) {
                seasons.add(seasonPath);
              }
            }
          });

        appendToResponse = "";
      }
    }

    return seasons;
  }

  private Serie.Season toSeason(JsonNode node, String parentId) {
    List<Serie.Episode> episodes = new ArrayList<>();

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

    return new Serie.Season(
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

  private Serie.Episode toEpisode(JsonNode node, String parentKey) {
    String releaseDate = node.path("air_date").textValue();
    Reception reception = node.get("vote_count").isNumber() && node.get("vote_average").isNumber()
      ? new Reception(node.get("vote_average").asDouble(), node.get("vote_count").asInt())
      : null;
    int seasonNumber = node.get("season_number").asInt();
    int episodeNumber = node.get("episode_number").asInt();
    WorkId id = new WorkId(DataSources.TMDB, MediaType.EPISODE, parentKey + "/" + seasonNumber + "/" + episodeNumber);

    return new Serie.Episode(
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
