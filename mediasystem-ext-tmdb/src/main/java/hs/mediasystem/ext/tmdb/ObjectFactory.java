package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.databind.JsonNode;

import hs.ddif.annotations.PluginScoped;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Classification;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Keyword;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.util.ImageURI;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

@PluginScoped
public class ObjectFactory {
  @Inject private TheMovieDatabase tmdb;

  public Production toProduction(JsonNode node, DataSource dataSource) throws IOException {
    ProductionIdentifier identifier = new ProductionIdentifier(dataSource, node.path("id").asText());

    return new Production(
      identifier,
      createDetails(node, identifier),
      createReception(node),
      new Classification(
        node.path("genres").findValuesAsText("name"),
        node.path("spoken_languages").findValuesAsText("name"),
        List.of(),
        Map.of(),
        node.path("adult").isBoolean() ? node.path("adult").booleanValue() : null
      ),
      node.path("popularity").doubleValue(),
      Set.of()
    );
  }

  public Movie toMovie(JsonNode node) throws IOException {
    Number runtime = node.path("runtime").numberValue();

    JsonNode collectionPath = node.path("belongs_to_collection");
    Set<Identifier> relatedIdentifiers = new HashSet<>();

    if(collectionPath.isObject()) {
      relatedIdentifiers.add(new Identifier(DataSources.TMDB_COLLECTION, collectionPath.path("id").asText()));
    }

    String imdbId = node.path("imdb_id").textValue();

    if(imdbId != null) {
      relatedIdentifiers.add(new Identifier(DataSources.IMDB_MOVIE, imdbId));
    }

    ProductionIdentifier identifier = new ProductionIdentifier(DataSources.TMDB_MOVIE, node.path("id").asText());

    return new Movie(
      identifier,
      createDetails(node, identifier),
      createReception(node),
      runtime == null ? null : Duration.ofMinutes(runtime.intValue()),
      new Classification(
        node.path("genres").findValuesAsText("name"),
        node.path("spoken_languages").findValuesAsText("name"),
        StreamSupport.stream(node.path("keywords").path("keywords").spliterator(), false).map(n -> new Keyword(new Identifier(DataSources.TMDB_KEYWORD, n.path("id").asText()), n.path("name").textValue())).collect(Collectors.toList()),
        extractCertifications(node.path("release_dates").path("results")),
        node.path("adult").isBoolean() ? node.path("adult").booleanValue() : null
      ),
      node.path("popularity").doubleValue(),
      node.path("tagline").isTextual() && node.path("tagline").textValue().isBlank() ? null : node.path("tagline").textValue(),
      toMovieState(node.path("status").textValue()),
      relatedIdentifiers
    );
  }

  public Serie toSerie(JsonNode node, List<Season> seasons) throws IOException {
    ProductionIdentifier identifier = new ProductionIdentifier(DataSources.TMDB_SERIE, node.path("id").asText());

    return new Serie(
      identifier,
      createDetails(node, identifier),
      createReception(node),
      new Classification(
        node.path("genres").findValuesAsText("name"),
        node.path("languages").findValuesAsText("name"),
        StreamSupport.stream(node.path("keywords").path("keywords").spliterator(), false).map(n -> new Keyword(new Identifier(DataSources.TMDB_KEYWORD, n.path("id").asText()), n.path("name").textValue())).collect(Collectors.toList()),
        extractCertifications(node.path("content_ratings").path("results")),
        node.path("adult").isBoolean() ? node.path("adult").booleanValue() : null
      ),
      toSerieState(node.path("status").textValue()),
      parseDate(node.path("last_air_date").textValue()),
      node.path("popularity").doubleValue(),
      seasons,
      Set.of()
    );
  }

  private Details createDetails(JsonNode node, Identifier identifier) throws IOException {
    String releaseDate = node.get("release_date") == null ? node.path("first_air_date").textValue() : node.get("release_date").textValue();
    ImageURI backdropURI = tmdb.createImageURI(node.path("backdrop_path").textValue(), "original", "image:backdrop:" + identifier.toString());
    ImageURI posterURI = tmdb.createImageURI(node.path("poster_path").textValue(), "original", "image:cover:" + identifier.toString());

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
