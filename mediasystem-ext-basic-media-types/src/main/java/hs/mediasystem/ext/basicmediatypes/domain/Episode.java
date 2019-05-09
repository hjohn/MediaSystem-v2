package hs.mediasystem.ext.basicmediatypes.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Episode extends Release {
  private final int seasonNumber;
  private final int number;
  private final List<PersonRole> personRoles;
  private final Duration duration;

  public Episode(ProductionIdentifier identifier, Details details, Reception reception, Duration duration, int seasonNumber, int number, List<PersonRole> personRoles) {
    super(identifier, details, reception);

    this.seasonNumber = seasonNumber;
    this.number = number;
    this.duration = duration;
    this.personRoles = new ArrayList<>(Collections.unmodifiableList(personRoles));
  }

  public int getSeasonNumber() {
    return seasonNumber;
  }

  public int getNumber() {
    return number;
  }

  public Duration getDuration() {
    return duration;
  }

  public List<PersonRole> getPersonRoles() {  // TODO Unused
    return personRoles;
  }
}
