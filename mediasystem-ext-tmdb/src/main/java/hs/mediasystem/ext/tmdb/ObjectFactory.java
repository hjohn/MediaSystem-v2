package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.domain.Chronology;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.util.ImageURI;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ObjectFactory {
  @Inject private TheMovieDatabase tmdb;

  public Movie toMovie(JsonNode node) {
    String releaseDate = node.path("release_date").textValue();
    Number runtime = node.path("runtime").numberValue();

    ImageURI backdropURI = tmdb.createImageURI(node.path("backdrop_path").textValue(), "original");
    ImageURI posterURI = tmdb.createImageURI(node.path("poster_path").textValue(), "original");

    Reception reception = node.get("vote_count").isNumber() && node.get("vote_average").isNumber() ?
      new Reception(node.get("vote_average").asDouble(), node.get("vote_count").asInt()) : null;

    JsonNode collectionPath = node.path("belongs_to_collection");
    Chronology chronology = null;

    if(!collectionPath.isMissingNode()) {
      chronology = new Chronology(
        new Identifier(DataSources.TMDB_CHRONOLOGY, collectionPath.path("id").asText()),
        collectionPath.path("name").asText(),
        tmdb.createImageURI(collectionPath.path("poster_path").textValue(), "original"),
        tmdb.createImageURI(collectionPath.path("backdrop_path").textValue(), "original")
      );
    }

    return new Movie(
      new ProductionIdentifier(DataSources.TMDB_MOVIE, node.path("id").asText()),
      new Details(
        node.get("title").textValue(),
        node.path("overview").textValue(),
        releaseDate == null ? null : LocalDate.parse(releaseDate, DateTimeFormatter.ISO_DATE),
        posterURI,
        backdropURI
      ),
      reception,
      runtime == null ? null : Duration.ofMinutes(runtime.intValue()),
      node.path("spoken_languages").findValuesAsText("name"),
      node.path("genres").findValuesAsText("name"),
      node.path("popularity").doubleValue(),
      node.path("tagline").textValue(),
      toMovieState(node.path("status").textValue()),
      chronology
    );
  }

  public Serie toSerie(JsonNode node, List<Season> seasons) {
    String releaseDate = node.path("first_air_date").textValue();
    String lastAirDate = node.path("last_air_date").textValue();
    Number runtime = node.path("episode_run_time").numberValue();

    ImageURI backdropURI = tmdb.createImageURI(node.path("backdrop_path").textValue(), "original");
    ImageURI posterURI = tmdb.createImageURI(node.path("poster_path").textValue(), "original");

    Reception reception = node.get("vote_count").isNumber() && node.get("vote_average").isNumber() ?
      new Reception(node.get("vote_average").asDouble(), node.get("vote_count").asInt()) : null;

    return new Serie(
      new ProductionIdentifier(DataSources.TMDB_SERIE, node.path("id").asText()),
      new Details(
        node.get("name").textValue(),
        node.path("overview").textValue(),
        releaseDate == null ? null : LocalDate.parse(releaseDate, DateTimeFormatter.ISO_DATE),
        posterURI,
        backdropURI
      ),
      reception,
      runtime == null ? null : Duration.ofMinutes(runtime.intValue()),
      node.path("languages").findValuesAsText("name"),
      node.path("genres").findValuesAsText("name"),
      toSerieState(node.path("status").textValue()),
      lastAirDate == null ? null : LocalDate.parse(lastAirDate, DateTimeFormatter.ISO_DATE),
      node.path("popularity").doubleValue(),
      seasons
    );
  }

  private static Movie.State toMovieState(String text) {
    if(text == null) {
      return null;
    }

    switch(text) {
    case "In Production":
      return Movie.State.IN_PRODUCTION;
    case "Planned":
      return Movie.State.PLANNED;
    case "Released":
      return Movie.State.RELEASED;
    default:
      return null;
    }
  }

  private static Serie.State toSerieState(String text) {
    if(text == null) {
      return null;
    }

    switch(text) {
    case "In Production":
      return Serie.State.IN_PRODUCTION;
    case "Planned":
      return Serie.State.PLANNED;
    case "Returning Series":
      return Serie.State.CONTINUING;
    case "Canceled":
      return Serie.State.CANCELED;
    case "Pilot":
      return Serie.State.PILOT;
    case "Ended":
      return Serie.State.ENDED;
    default:
      return null;
    }
  }
}
