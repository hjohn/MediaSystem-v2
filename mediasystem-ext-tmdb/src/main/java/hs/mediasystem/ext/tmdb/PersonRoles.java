package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Person;
import hs.mediasystem.ext.basicmediatypes.domain.PersonIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.PersonRole;
import hs.mediasystem.ext.basicmediatypes.domain.Role;
import hs.mediasystem.util.ImageURI;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PersonRoles {
  private static final double GUEST_STAR_MINIMUM_VALUE = 1e6;
  private static final double CREW_MINIMUM_VALUE = 1e12;

  @Inject private TheMovieDatabase tmdb;

  // TODO use "order", maybe "gender"
  public List<PersonRole> toPersonRoles(JsonNode node) {
    List<PersonRole> roles = new ArrayList<>();

    for(JsonNode cast : node.path("cast")) {
      ImageURI imageURI = tmdb.createImageURI(cast.path("profile_path").textValue(), "original");

      roles.add(new PersonRole(
        new Person(new PersonIdentifier(DataSources.TMDB_PERSON, cast.get("id").asText()), cast.get("name").asText(), imageURI),
        Role.asCast(new Identifier(DataSources.TMDB_CREDIT, cast.get("credit_id").asText()), cast.get("character").asText()),
        cast.get("order").asDouble()
      ));
    }

    for(JsonNode guestStar : node.path("guest_stars")) {
      ImageURI imageURI = tmdb.createImageURI(guestStar.path("profile_path").textValue(), "original");

      roles.add(new PersonRole(
        new Person(new PersonIdentifier(DataSources.TMDB_PERSON, guestStar.get("id").asText()), guestStar.get("name").asText(), imageURI),
        Role.asGuestStar(new Identifier(DataSources.TMDB_CREDIT, guestStar.get("credit_id").asText()), guestStar.get("character").asText()),
        guestStar.get("order").asDouble() + GUEST_STAR_MINIMUM_VALUE
      ));
    }

    int crewCount = 0;

    for(JsonNode crew : node.path("crew")) {
      ImageURI imageURI = tmdb.createImageURI(crew.path("profile_path").textValue(), "original");

      roles.add(new PersonRole(
        new Person(new PersonIdentifier(DataSources.TMDB_PERSON, crew.get("id").asText()), crew.get("name").asText(), imageURI),
        Role.asCrew(new Identifier(DataSources.TMDB_CREDIT, crew.get("credit_id").asText()), crew.get("department").asText(), crew.get("job").asText()),
        CREW_MINIMUM_VALUE + crewCount++
      ));
    }

    return roles;
  }
}
