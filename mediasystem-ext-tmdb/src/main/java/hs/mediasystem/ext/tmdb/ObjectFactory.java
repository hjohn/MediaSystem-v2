package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.api.datasource.domain.Classification;
import hs.mediasystem.api.datasource.domain.Details;
import hs.mediasystem.api.datasource.domain.Keyword;
import hs.mediasystem.api.datasource.domain.Movie;
import hs.mediasystem.api.datasource.domain.Production;
import hs.mediasystem.api.datasource.domain.Serie;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Context;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.KeywordId;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.image.ImageURI;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ObjectFactory {
  @Inject private TheMovieDatabase tmdb;

  public Production toProduction(JsonNode node, DataSource dataSource, MediaType mediaType) {
    WorkId id = new WorkId(dataSource, mediaType, node.path("id").asText());

    return new Production(
      id,
      createDetails(node, id),
      createReception(node),
      null,
      node.path("tagline").isTextual() && node.path("tagline").textValue().isBlank() ? null : node.path("tagline").textValue(),
      new Classification(
        node.path("genres").findValuesAsText("name"),
        node.path("spoken_languages").findValuesAsText("name"),
        List.of(),
        Map.of(),
        node.path("adult").isBoolean() ? node.path("adult").booleanValue() : null
      ),
      node.path("popularity").doubleValue()
    );
  }

  public Movie toMovie(JsonNode node) {
    Number runtime = node.path("runtime").numberValue();

    JsonNode collectionPath = node.path("belongs_to_collection");
    Context context = null;

    if(collectionPath.isObject()) {
      WorkId id = new WorkId(DataSources.TMDB, MediaType.COLLECTION, collectionPath.path("id").asText());

      context = new Context(
        id,
        collectionPath.path("name").asText(),
        Optional.ofNullable(tmdb.createImageURI(node.path("poster_path").textValue(), "original", "image:cover:" + id.toString())),
        Optional.ofNullable(tmdb.createImageURI(node.path("backdrop_path").textValue(), "original", "image:backdrop:" + id.toString()))
      );
    }

    WorkId id = new WorkId(DataSources.TMDB, MediaType.MOVIE, node.path("id").asText());

    return new Movie(
      id,
      createDetails(node, id),
      createReception(node),
      context,
      node.path("tagline").isTextual() && node.path("tagline").textValue().isBlank() ? null : node.path("tagline").textValue(),
      runtime == null ? null : Duration.ofMinutes(runtime.intValue()),
      new Classification(
        node.path("genres").findValuesAsText("name"),
        node.path("spoken_languages").findValuesAsText("name"),
        StreamSupport.stream(node.path("keywords").path("keywords").spliterator(), false).map(n -> new Keyword(new KeywordId(DataSources.TMDB, n.path("id").asText()), n.path("name").textValue())).collect(Collectors.toList()),
        extractCertifications(node.path("release_dates").path("results")),
        node.path("adult").isBoolean() ? node.path("adult").booleanValue() : null
      ),
      node.path("popularity").doubleValue(),
      toMovieState(node.path("status").textValue())
    );
  }

  public Serie toSerie(JsonNode node, List<Serie.Season> seasons) {
    WorkId id = new WorkId(DataSources.TMDB, MediaType.SERIE, node.path("id").asText());

    return new Serie(
      id,
      createDetails(node, id),
      createReception(node),
      node.path("tagline").isTextual() && node.path("tagline").textValue().isBlank() ? null : node.path("tagline").textValue(),
      new Classification(
        node.path("genres").findValuesAsText("name"),
        node.path("languages").findValuesAsText("name"),
        StreamSupport.stream(node.path("keywords").path("keywords").spliterator(), false).map(n -> new Keyword(new KeywordId(DataSources.TMDB, n.path("id").asText()), n.path("name").textValue())).collect(Collectors.toList()),
        extractCertifications(node.path("content_ratings").path("results")),
        node.path("adult").isBoolean() ? node.path("adult").booleanValue() : null
      ),
      toSerieState(node.path("status").textValue()),
      parseDate(node.path("last_air_date").textValue()),
      node.path("popularity").doubleValue(),
      seasons
    );
  }

  private Details createDetails(JsonNode node, WorkId id) {
    String releaseDate = node.get("release_date") == null ? node.path("first_air_date").textValue() : node.get("release_date").textValue();
    ImageURI backdropURI = tmdb.createImageURI(node.path("backdrop_path").textValue(), "original", "image:backdrop:" + id.toString());
    ImageURI posterURI = tmdb.createImageURI(node.path("poster_path").textValue(), "original", "image:cover:" + id.toString());

    return new Details(
      Optional.ofNullable(node.get("title")).or(() -> Optional.ofNullable(node.get("name"))).map(JsonNode::textValue).orElse("(untitled)"),
      null,
      node.path("overview").textValue(),
      parseDate(releaseDate),
      posterURI,
      null,
      backdropURI
    );
  }

  private static Reception createReception(JsonNode node) {
    return node.at("/vote_count").isNumber() && node.at("/vote_average").isNumber() ?
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

  private static Map<String, String> extractCertifications(JsonNode root) {
    Map<String, String> certifications = new HashMap<>();

    for(JsonNode node : root) {
      String countryCode = node.path("iso_3166_1").asText();
      String certification = node.path("rating").asText();
      int bestType = Integer.MAX_VALUE;

      for(JsonNode releaseDateNode : node.path("release_dates")) {
        int type = releaseDateNode.path("type").asInt();

        if(type < bestType) {
          certification = releaseDateNode.path("certification").asText();
          bestType = type;
        }
      }

      if(certification != null && countryCode != null) {
        certifications.put(countryCode, certification);
      }
    }

    return certifications;
  }
}
