package hs.mediasystem.ext.basicmediatypes.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Episode extends Production {
  private final int seasonNumber;
  private final int number;
  private final List<PersonRole> personRoles;

  public Episode(ProductionIdentifier identifier, Details details, Reception reception, Duration duration, int seasonNumber, int number, List<PersonRole> personRoles) {
    super(identifier, details, reception, duration, Collections.emptyList(), Collections.emptyList());

    this.seasonNumber = seasonNumber;
    this.number = number;
    this.personRoles = new ArrayList<>(Collections.unmodifiableList(personRoles));
  }

  public int getSeasonNumber() {
    return seasonNumber;
  }

  public int getNumber() {
    return number;
  }

  public List<PersonRole> getPersonRoles() {
    return personRoles;
  }
}
