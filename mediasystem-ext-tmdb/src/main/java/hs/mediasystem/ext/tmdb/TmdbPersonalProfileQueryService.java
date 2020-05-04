package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Classification;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Person;
import hs.mediasystem.ext.basicmediatypes.domain.PersonIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.PersonalProfile;
import hs.mediasystem.ext.basicmediatypes.domain.PersonalProfile.Gender;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionRole;
import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.ext.basicmediatypes.services.PersonalProfileQueryService;
import hs.mediasystem.util.ImageURI;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TmdbPersonalProfileQueryService implements PersonalProfileQueryService {
  @Inject private TheMovieDatabase tmdb;

  @Override
  public PersonalProfile query(PersonIdentifier identifier) throws IOException {
    JsonNode node = tmdb.query("3/person/" + identifier.getId(), "text:json:" + identifier, List.of("append_to_response", "combined_credits"));
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
      new Person(identifier, node.path("name").textValue(), tmdb.createImageURI(node.path("profile_path").textValue(), "original", "image:person:" + identifier)),
      gender == 1 ? Gender.FEMALE : gender == 2 ? Gender.MALE : null,
      node.path("popularity").doubleValue(),
      node.path("place_of_birth").textValue(),
      birthDay == null ? null : LocalDate.parse(birthDay, DateTimeFormatter.ISO_DATE),
      deathDay == null ? null : LocalDate.parse(deathDay, DateTimeFormatter.ISO_DATE),
      node.path("biography").textValue(),
      roles
    );
  }

  private ProductionRole createProductionRole(JsonNode node, boolean isCast) throws IOException {
    boolean isMovie = node.get("media_type").textValue().equals("movie");
    ProductionIdentifier identifier = new ProductionIdentifier(DataSource.instance(isMovie ? MediaType.MOVIE : MediaType.SERIE, "TMDB"), node.get("id").asText());
    ImageURI backdropURI = tmdb.createImageURI(node.path("backdrop_path").textValue(), "original", "image:backdrop:" + identifier);
    ImageURI posterURI = tmdb.createImageURI(node.path("poster_path").textValue(), "original", "image:cover:" + identifier);

    String releaseDate = node.has("release_date") ? node.path("release_date").textValue() : node.path("first_air_date").textValue();

    Reception reception = node.get("vote_count").isNumber() && node.get("vote_average").isNumber() ?
      new Reception(node.get("vote_average").asDouble(), node.get("vote_count").asInt()) : null;

    List<String> genres = new ArrayList<>();

    for(JsonNode element : node.path("genre_ids")) {
      genres.add(Genres.toString(element.asInt()));
    }

    Production production = new Production(
      identifier,
      new Details(
        isMovie ? node.path("title").textValue() : node.path("name").textValue(),
        null,
        node.path("overview").textValue(),
        TheMovieDatabase.parseDateOrNull(releaseDate),
        posterURI,
        backdropURI
      ),
      reception,
      new Classification(
        genres,
        List.of(),
        List.of(),
        Map.of(),
        node.path("adult").isBoolean() ? node.path("adult").booleanValue() : null
      ),
      node.path("popularity").doubleValue(),
      Set.of()
    );

    Identifier roleIdentifier = new Identifier(DataSources.TMDB_CREDIT, node.get("credit_id").asText());

    Role role = isCast ? Role.asCast(roleIdentifier, node.path("character").textValue())
                       : Role.asCrew(roleIdentifier, node.path("department").textValue(), node.path("job").textValue());

    return new ProductionRole(production, role, node.has("episode_count") ? node.get("episode_count").asInt() : null, node.get("popularity").asDouble());
  }
}
