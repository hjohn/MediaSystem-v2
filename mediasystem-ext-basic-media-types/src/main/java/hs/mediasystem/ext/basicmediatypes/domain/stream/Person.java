package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.util.ImageURI;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class Person {
  public enum Gender {
    MALE, FEMALE
  }

  private final String name;
  private final Optional<String> biography;
  private final Optional<ImageURI> image;
  private final double popularity;
  private final Optional<Gender> gender;
  private final Optional<String> birthPlace;
  private final Optional<LocalDate> birthDate;
  private final Optional<LocalDate> deathDate;
  private final List<Participation> participations;

  public Person(String name, String biography, ImageURI image, Gender gender, double popularity, String birthPlace, LocalDate birthDate, LocalDate deathDate, List<Participation> participations) {
    if(name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    if(participations == null) {
      throw new IllegalArgumentException("participations cannot be null");
    }

    this.name = name;
    this.biography = Optional.ofNullable(biography);
    this.image = Optional.ofNullable(image);
    this.popularity = popularity;
    this.gender = Optional.ofNullable(gender);
    this.birthPlace = Optional.ofNullable(birthPlace);
    this.birthDate = Optional.ofNullable(birthDate);
    this.deathDate = Optional.ofNullable(deathDate);
    this.participations = participations;
  }

  public String getName() {
    return name;
  }

  public Optional<String> getBiography() {
    return biography;
  }

  public Optional<ImageURI> getImage() {
    return image;
  }

  public Optional<Gender> getGender() {
    return gender;
  }

  public double getPopularity() {
    return popularity;
  }

  public Optional<LocalDate> getBirthDate() {
    return birthDate;
  }

  public Optional<LocalDate> getDeathDate() {
    return deathDate;
  }

  public Optional<String> getBirthPlace() {
    return birthPlace;
  }

  public List<Participation> getParticipations() {
    return participations;
  }
}
