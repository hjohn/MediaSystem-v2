package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.CollectionDetails;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Keyword;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.util.ImageURI;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ObjectFactory {
  @Inject private TheMovieDatabase tmdb;

  public Production toProduction(JsonNode node, DataSource dataSource) {
    return new Production(
      new ProductionIdentifier(dataSource, node.path("id").asText()),
      createDetails(node),
      createReception(node),
      node.path("spoken_languages").findValuesAsText("name"),
      node.path("genres").findValuesAsText("name"),
      node.path("popularity").doubleValue()
    );
  }

  public Movie toMovie(JsonNode node) {
    Number runtime = node.path("runtime").numberValue();

    JsonNode collectionPath = node.path("belongs_to_collection");
    CollectionDetails collectionDetails = null;

    if(collectionPath.isObject()) {
      collectionDetails = new CollectionDetails(
        new Identifier(DataSources.TMDB_COLLECTION, collectionPath.path("id").asText()),
        new Details(
          collectionPath.path("name").asText(),
          null,
          null,
          tmdb.createImageURI(collectionPath.path("poster_path").textValue(), "original"),
          tmdb.createImageURI(collectionPath.path("backdrop_path").textValue(), "original")
        )
      );
    }

    return new Movie(
      new ProductionIdentifier(DataSources.TMDB_MOVIE, node.path("id").asText()),
      createDetails(node),
      createReception(node),
      runtime == null ? null : Duration.ofMinutes(runtime.intValue()),
      node.path("spoken_languages").findValuesAsText("name"),
      node.path("genres").findValuesAsText("name"),
      StreamSupport.stream(node.path("keywords").path("keywords").spliterator(), false).map(n -> new Keyword(new Identifier(DataSources.TMDB_KEYWORD, n.path("id").asText()), n.path("name").textValue())).collect(Collectors.toList()),
      node.path("popularity").doubleValue(),
      node.path("tagline").textValue(),
      toMovieState(node.path("status").textValue()),
      collectionDetails
    );
  }

  public Serie toSerie(JsonNode node, List<Season> seasons) {
    return new Serie(
      new ProductionIdentifier(DataSources.TMDB_SERIE, node.path("id").asText()),
      createDetails(node),
      createReception(node),
      node.path("languages").findValuesAsText("name"),
      node.path("genres").findValuesAsText("name"),
      StreamSupport.stream(node.path("keywords").path("keywords").spliterator(), false).map(n -> new Keyword(new Identifier(DataSources.TMDB_KEYWORD, n.path("id").asText()), n.path("name").textValue())).collect(Collectors.toList()),
      toSerieState(node.path("status").textValue()),
      parseDate(node.path("last_air_date").textValue()),
      node.path("popularity").doubleValue(),
      seasons
    );
  }

  private Details createDetails(JsonNode node) {
    String releaseDate = node.get("release_date") == null ? node.path("first_air_date").textValue() : node.get("release_date").textValue();
    ImageURI backdropURI = tmdb.createImageURI(node.path("backdrop_path").textValue(), "original");
    ImageURI posterURI = tmdb.createImageURI(node.path("poster_path").textValue(), "original");

    return new Details(
      node.get("title") == null ? node.get("name").textValue() : node.get("title").textValue(),
      node.path("overview").textValue(),
      parseDate(releaseDate),
      posterURI,
      backdropURI
    );
  }

  private static Reception createReception(JsonNode node) {
    return node.get("vote_count").isNumber() && node.get("vote_average").isNumber() ?
      new Reception(node.get("vote_average").asDouble(), node.get("vote_count").asInt()) : null;
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

  public LocalDate parseDate(String text) {
    return TheMovieDatabase.parseDateOrNull(text);
  }
}
