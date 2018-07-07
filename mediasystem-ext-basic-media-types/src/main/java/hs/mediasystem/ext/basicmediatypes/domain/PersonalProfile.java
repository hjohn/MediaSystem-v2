package hs.mediasystem.ext.basicmediatypes.domain;

import java.time.LocalDate;

public class PersonalProfile {
  private final Person person;
  private final LocalDate birthDate;
  private final String biography;

  public PersonalProfile(Person person, LocalDate birthDate, String biography) {
    if(person == null) {
      throw new IllegalArgumentException("person cannot be null");
    }
    if(birthDate == null) {
      throw new IllegalArgumentException("birthDate cannot be null");
    }

    this.person = person;
    this.birthDate = birthDate;
    this.biography = biography;
  }

  public Person getPerson() {
    return person;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public String getBiography() {
    return biography;
  }
}
