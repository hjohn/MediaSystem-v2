package hs.mediasystem.ext.basicmediatypes.domain;

import java.time.LocalDate;
import java.util.List;

public class PersonalProfile {
  public enum Gender {
    MALE, FEMALE
  }

  private final Person person;
  private final double popularity;
  private final Gender gender;
  private final String birthPlace;
  private final LocalDate birthDate;
  private final LocalDate deathDate;
  private final String biography;
  private final List<ProductionRole> productionRoles;

  public PersonalProfile(Person person, Gender gender, double popularity, String birthPlace, LocalDate birthDate, LocalDate deathDate, String biography, List<ProductionRole> productionRoles) {
    if(person == null) {
      throw new IllegalArgumentException("person cannot be null");
    }
    if(birthDate == null) {
      throw new IllegalArgumentException("birthDate cannot be null");
    }
    if(productionRoles == null) {
      throw new IllegalArgumentException("productionRoles cannot be null");
    }

    this.person = person;
    this.popularity = popularity;
    this.gender = gender;
    this.birthPlace = birthPlace;
    this.birthDate = birthDate;
    this.deathDate = deathDate;
    this.biography = biography;
    this.productionRoles = productionRoles;
  }

  public Person getPerson() {
    return person;
  }

  public String getBirthPlace() {
    return birthPlace;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public LocalDate getDeathDate() {
    return deathDate;
  }

  public String getBiography() {
    return biography;
  }

  public List<ProductionRole> getProductionRoles() {
    return productionRoles;
  }
}
