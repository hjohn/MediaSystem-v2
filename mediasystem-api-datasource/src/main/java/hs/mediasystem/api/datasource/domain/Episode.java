package hs.mediasystem.api.datasource.domain;

import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Episode extends Release {
  private final int seasonNumber;
  private final int number;
  private final List<PersonRole> personRoles;
  private final Duration duration;

  public Episode(WorkId id, Details details, Reception reception, Duration duration, int seasonNumber, int number, List<PersonRole> personRoles) {
    super(id, details, reception);

    if(number < 0) {
      throw new IllegalArgumentException("number must not be negative: " + number);
    }

    this.seasonNumber = seasonNumber;
    this.number = number;
    this.duration = duration;
    this.personRoles = new ArrayList<>(Collections.unmodifiableList(personRoles));
  }

  public int getSeasonNumber() {
    return seasonNumber;
  }

  /**
   * The number of the episode.
   *
   * @return the number of the episode, never negative
   */
  public int getNumber() {
    return number;
  }

  public Duration getDuration() {
    return duration;
  }

  public List<PersonRole> getPersonRoles() {  // TODO Unused, perhaps useful to create a consolidated view of all cast and crew for a serie
    return personRoles;
  }
}
