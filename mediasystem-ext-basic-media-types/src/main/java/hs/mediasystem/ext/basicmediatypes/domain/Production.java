package hs.mediasystem.ext.basicmediatypes.domain;

import java.util.List;

/**
 * Represents either a Movie, a TV Movie or an entire TV Series.
 */
public class Production extends Release {
  private final List<String> languages;
  private final List<String> genres;
  private final double popularity;

  public Production(ProductionIdentifier identifier, Details details, Reception reception, List<String> languages, List<String> genres, double popularity) {
    super(identifier, details, reception);

    if(languages == null) {
      throw new IllegalArgumentException("languages cannot be null");
    }
    if(genres == null) {
      throw new IllegalArgumentException("genres cannot be null");
    }

    this.popularity = popularity;
    this.languages = languages;
    this.genres = genres;
  }

  public List<String> getLanguages() {
    return languages;
  }

  public List<String> getGenres() {
    return genres;
  }

  public double getPopularity() {
    return popularity;
  }

  @Override
  public String toString() {
    return "Production[" + super.toString() + "]";
  }
}
