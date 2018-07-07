package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Type;
import hs.mediasystem.ext.basicmediatypes.domain.PersonIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionRole;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.ext.basicmediatypes.services.ParticipationsQueryService;
import hs.mediasystem.ext.tmdb.Genres;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;
import hs.mediasystem.util.ImageURI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class TmdbParticipationsQueryService implements ParticipationsQueryService {
  private static final Type MOVIE = Type.of("MOVIE");
  private static final Type SERIE = Type.of("SERIE");

  @Inject private TheMovieDatabase tmdb;

  @Override
  public List<ProductionRole> query(PersonIdentifier identifier) {
    JsonNode info = tmdb.query("3/person/" + identifier.getId() + "/combined_credits");
    List<ProductionRole> roles = new ArrayList<>();

    for(JsonNode crew : info.path("crew")) {
      roles.add(createProductionRole(crew, false));
    }

    for(JsonNode cast : info.path("cast")) {
      roles.add(createProductionRole(cast, true));
    }

    return roles;
  }

  private ProductionRole createProductionRole(JsonNode node, boolean isCast) {
    ImageURI backdropURI = tmdb.createImageURI(node.path("backdrop_path").textValue(), "original");
    ImageURI posterURI = tmdb.createImageURI(node.path("poster_path").textValue(), "original");

    String releaseDate = node.has("release_date") ? node.path("release_date").textValue() : node.path("first_air_date").textValue();

    Reception reception = node.get("vote_count").isNumber() && node.get("vote_average").isNumber() ?
      new Reception(node.get("vote_average").asDouble(), node.get("vote_count").asInt()) : null;

    boolean isMovie = node.get("media_type").textValue().equals("movie");
    List<String> genres = new ArrayList<>();

    for(JsonNode element : node.path("genre_ids")) {
      genres.add(Genres.toString(element.asInt()));
    }

    Production production = new Production(
      new ProductionIdentifier(DataSource.instance(isMovie ? MOVIE : SERIE, "TMDB"), node.get("id").asText()),
      isMovie ? node.path("title").textValue() : node.path("name").textValue(),
      node.path("overview").textValue(),
      releaseDate == null || releaseDate.isEmpty() ? null : LocalDate.parse(releaseDate, DateTimeFormatter.ISO_DATE),
      posterURI,
      backdropURI,
      reception,
      null,
      Collections.emptyList(),
      genres
    );

    Role role = isCast ? Role.asCast(node.path("character").textValue())
                       : Role.asCrew(node.path("department").textValue(), node.path("job").textValue());

    return new ProductionRole(production, role, node.has("episode_count") ? node.get("episode_count").asInt() : null, node.get("popularity").asDouble());
  }
}
