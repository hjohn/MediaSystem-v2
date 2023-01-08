package hs.mediasystem.ext.tmdb;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.api.datasource.domain.Person;
import hs.mediasystem.api.datasource.domain.PersonRole;
import hs.mediasystem.api.datasource.domain.Role;
import hs.mediasystem.domain.work.PersonId;
import hs.mediasystem.domain.work.RoleId;
import hs.mediasystem.util.exception.Throwables;
import hs.mediasystem.util.image.ImageURI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PersonRoles {
  private static final Logger LOGGER = Logger.getLogger(PersonRoles.class.getName());
  private static final double GUEST_STAR_MINIMUM_VALUE = 1e6;
  private static final double CREW_MINIMUM_VALUE = 1e12;

  @Inject private TheMovieDatabase tmdb;

  // TODO use "order", maybe "gender"
  public List<PersonRole> toPersonRoles(JsonNode node) throws IOException {
    List<PersonRole> roles = new ArrayList<>();

    for(JsonNode cast : node.path("cast")) {
      try {
        PersonId id = new PersonId(DataSources.TMDB, cast.get("id").asText());
        ImageURI imageURI = tmdb.createImageURI(cast.path("profile_path").textValue(), "original", "image:person:" + id);

        roles.add(new PersonRole(
          new Person(id, cast.get("name").asText(), imageURI),
          Role.asCast(new RoleId(DataSources.TMDB, cast.get("credit_id").asText()), cast.get("character").asText()),
          cast.get("order").asDouble()
        ));
      }
      catch(RuntimeException e) {
        LOGGER.warning("Skipping Cast entry, exception while parsing: " + cast + ": " + Throwables.formatAsOneLine(e));
      }
    }

    for(JsonNode guestStar : node.path("guest_stars")) {
      try {
        PersonId id = new PersonId(DataSources.TMDB, guestStar.get("id").asText());
        ImageURI imageURI = tmdb.createImageURI(guestStar.path("profile_path").textValue(), "original", "image:person:" + id);

        roles.add(new PersonRole(
          new Person(id, guestStar.get("name").asText(), imageURI),
          Role.asGuestStar(new RoleId(DataSources.TMDB, guestStar.get("credit_id").asText()), guestStar.get("character").asText()),
          guestStar.get("order").asDouble() + GUEST_STAR_MINIMUM_VALUE
        ));
      }
      catch(RuntimeException e) {
        LOGGER.warning("Skipping Guest Star entry, exception while parsing: " + guestStar + ": " + Throwables.formatAsOneLine(e));
      }
    }

    int crewCount = 0;

    for(JsonNode crew : node.path("crew")) {
      try {
        PersonId id = new PersonId(DataSources.TMDB, crew.get("id").asText());
        ImageURI imageURI = tmdb.createImageURI(crew.path("profile_path").textValue(), "original", "image:person:" + id);

        roles.add(new PersonRole(
          new Person(id, crew.get("name").asText(), imageURI),
          Role.asCrew(new RoleId(DataSources.TMDB, crew.get("credit_id").asText()), crew.get("department").asText(), crew.get("job").asText()),
          CREW_MINIMUM_VALUE + crewCount++
        ));
      }
      catch(RuntimeException e) {
        LOGGER.warning("Skipping Crew entry, exception while parsing: " + crew + ": " + Throwables.formatAsOneLine(e));
      }
    }

    return roles;
  }
}
