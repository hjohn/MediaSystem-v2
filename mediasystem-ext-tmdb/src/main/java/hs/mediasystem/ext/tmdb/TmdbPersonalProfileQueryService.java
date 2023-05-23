package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.api.datasource.domain.Classification;
import hs.mediasystem.api.datasource.domain.Details;
import hs.mediasystem.api.datasource.domain.Person;
import hs.mediasystem.api.datasource.domain.PersonalProfile;
import hs.mediasystem.api.datasource.domain.PersonalProfile.Gender;
import hs.mediasystem.api.datasource.domain.Production;
import hs.mediasystem.api.datasource.domain.ProductionRole;
import hs.mediasystem.api.datasource.domain.Role;
import hs.mediasystem.api.datasource.services.PersonalProfileQueryService;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.PersonId;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.RoleId;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.exception.Throwables;
import hs.mediasystem.util.image.ImageURI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TmdbPersonalProfileQueryService implements PersonalProfileQueryService {
  private static final Logger LOGGER = Logger.getLogger(TmdbPersonalProfileQueryService.class.getName());

  @Inject private TheMovieDatabase tmdb;

  @Override
  public PersonalProfile query(PersonId id) throws IOException {
    JsonNode node = tmdb.query("3/person/" + id.getKey(), "text:json:" + id, List.of("append_to_response", "combined_credits"));
    String birthDay = node.path("birthday").textValue();
    String deathDay = node.path("deathday").textValue();
    int gender = node.path("gender").intValue();

    JsonNode info = node.path("combined_credits");

    List<ProductionRole> roles = new ArrayList<>();

    for(JsonNode crew : info.path("crew")) {
      try {
        roles.add(createProductionRole(crew, false));
      }
      catch(RuntimeException e) {
        LOGGER.warning("Skipping Crew entry, exception while parsing: " + crew + ": " + Throwables.formatAsOneLine(e));
      }
    }

    for(JsonNode cast : info.path("cast")) {
      try {
        roles.add(createProductionRole(cast, true));
      }
      catch(RuntimeException e) {
        LOGGER.warning("Skipping Cast entry, exception while parsing: " + cast + ": " + Throwables.formatAsOneLine(e));
      }
    }

    return new PersonalProfile(
      new Person(id, node.path("name").textValue(), tmdb.createImageURI(node.path("profile_path").textValue(), "original", "image:person:" + id)),
      gender == 1 ? Gender.FEMALE : gender == 2 ? Gender.MALE : null,
      node.path("popularity").doubleValue(),
      node.path("place_of_birth").textValue(),
      TheMovieDatabase.parseDateOrNull(birthDay),
      TheMovieDatabase.parseDateOrNull(deathDay),
      node.path("biography").textValue(),
      roles
    );
  }

  private ProductionRole createProductionRole(JsonNode node, boolean isCast) throws IOException {
    boolean isMovie = node.get("media_type").textValue().equals("movie");
    WorkId id = new WorkId(DataSources.TMDB, isMovie ? MediaType.MOVIE : MediaType.SERIE, node.get("id").asText());
    ImageURI backdropURI = tmdb.createImageURI(node.path("backdrop_path").textValue(), "original", "image:backdrop:" + id);
    ImageURI posterURI = tmdb.createImageURI(node.path("poster_path").textValue(), "original", "image:cover:" + id);

    String releaseDate = node.has("release_date") ? node.path("release_date").textValue() : node.path("first_air_date").textValue();

    Reception reception = node.get("vote_count").isNumber() && node.get("vote_average").isNumber() ?
      new Reception(node.get("vote_average").asDouble(), node.get("vote_count").asInt()) : null;

    List<String> genres = new ArrayList<>();

    for(JsonNode element : node.path("genre_ids")) {
      genres.add(Genres.toString(element.asInt()));
    }

    Production production = new Production(
      id,
      new Details(
        isMovie ? node.path("title").textValue() : node.path("name").textValue(),
        null,
        node.path("overview").textValue(),
        TheMovieDatabase.parseDateOrNull(releaseDate),
        posterURI,
        null,
        backdropURI
      ),
      reception,
      null,
      null,  // tag line is not part of this response
      new Classification(
        genres,
        List.of(),
        List.of(),
        Map.of(),
        node.path("adult").isBoolean() ? node.path("adult").booleanValue() : null
      ),
      node.path("popularity").doubleValue()
    );

    RoleId roleId = new RoleId(DataSources.TMDB, node.get("credit_id").asText());

    Role role = isCast ? Role.asCast(roleId, node.path("character").textValue())
                       : Role.asCrew(roleId, node.path("department").textValue(), node.path("job").textValue());

    return new ProductionRole(production, role, node.has("episode_count") ? node.get("episode_count").asInt() : null, node.get("popularity").asDouble());
  }
}
