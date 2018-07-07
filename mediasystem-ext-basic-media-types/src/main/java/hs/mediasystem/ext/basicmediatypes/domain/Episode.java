package hs.mediasystem.ext.basicmediatypes.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Episode {
  private final int seasonNumber;
  private final int number;
  private final Production production;
  private final List<PersonRole> personRoles;

  public Episode(int seasonNumber, int number, Production production, List<PersonRole> personRoles) {
    this.seasonNumber = seasonNumber;
    this.number = number;
    this.production = production;
    this.personRoles = new ArrayList<>(Collections.unmodifiableList(personRoles));
  }

  public int getSeasonNumber() {
    return seasonNumber;
  }

  public int getNumber() {
    return number;
  }

  public Production getProduction() {
    return production;
  }

  public List<PersonRole> getPersonRoles() {
    return personRoles;
  }
}
