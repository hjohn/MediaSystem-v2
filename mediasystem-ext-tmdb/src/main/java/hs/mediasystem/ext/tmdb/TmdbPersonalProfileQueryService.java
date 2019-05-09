package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Person;
import hs.mediasystem.ext.basicmediatypes.domain.PersonIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.PersonalProfile;
import hs.mediasystem.ext.basicmediatypes.domain.PersonalProfile.Gender;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionRole;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.ext.basicmediatypes.services.PersonalProfileQueryService;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.ImageURI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TmdbPersonalProfileQueryService implements PersonalProfileQueryService {
  private static final MediaType MOVIE = MediaType.of("MOVIE");
  private static final MediaType SERIE = MediaType.of("SERIE");

  @Inject private TheMovieDatabase tmdb;

  @Override
  public PersonalProfile query(PersonIdentifier identifier) {
    JsonNode node = tmdb.query("3/person/" + identifier.getId(), "append_to_response", "combined_credits");
    String birthDay = node.path("birthday").textValue();
    String deathDay = node.path("deathday").textValue();
    int gender = node.path("gender").intValue();

    JsonNode info = node.path("combined_credits");

    List<ProductionRole> roles = new ArrayList<>();

    for(JsonNode crew : info.path("crew")) {
      roles.add(createProductionRole(crew, false));
    }

    for(JsonNode cast : info.path("cast")) {
      roles.add(createProductionRole(cast, true));
    }

    return new PersonalProfile(
      new Person(identifier, node.path("name").textValue(), tmdb.createImageURI(node.path("profile_path").textValue(), "original")),
      gender == 1 ? Gender.FEMALE : gender == 2 ? Gender.MALE : null,
      node.path("popularity").doubleValue(),
      node.path("place_of_birth").textValue(),
      birthDay == null ? null : LocalDate.parse(birthDay, DateTimeFormatter.ISO_DATE),
      deathDay == null ? null : LocalDate.parse(deathDay, DateTimeFormatter.ISO_DATE),
      node.path("biography").textValue(),
      roles
    );
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
      new Details(
        isMovie ? node.path("title").textValue() : node.path("name").textValue(),
        node.path("overview").textValue(),
        TheMovieDatabase.parseDateOrNull(releaseDate),
        posterURI,
        backdropURI
      ),
      reception,
      Collections.emptyList(),
      genres,
      node.path("popularity").doubleValue()
    );

    Identifier identifier = new Identifier(DataSources.TMDB_CREDIT, node.get("credit_id").asText());

    Role role = isCast ? Role.asCast(identifier, node.path("character").textValue())
                       : Role.asCrew(identifier, node.path("department").textValue(), node.path("job").textValue());

    return new ProductionRole(production, role, node.has("episode_count") ? node.get("episode_count").asInt() : null, node.get("popularity").asDouble());
  }
}
