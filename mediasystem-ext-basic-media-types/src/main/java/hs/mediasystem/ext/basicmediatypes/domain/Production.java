package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Reception;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Represents either a Movie, a TV Movie or an entire TV Series.
 */
public class Production extends Release {
  private final List<String> languages;
  private final List<String> genres;
  private final double popularity;
  private final Set<Identifier> relatedIdentifiers;

  public Production(ProductionIdentifier identifier, Details details, Reception reception, List<String> languages, List<String> genres, double popularity, Set<Identifier> relatedIdentifiers) {
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
    this.relatedIdentifiers = Set.copyOf(relatedIdentifiers);
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

  public Set<Identifier> getRelatedIdentifiers() {
    return relatedIdentifiers;
  }

  public Optional<Identifier> getCollectionIdentifier() {
    return relatedIdentifiers.stream().filter(i -> i.getDataSource().getType().equals(MediaType.COLLECTION)).findAny();
  }

  @Override
  public String toString() {
    return "Production[" + super.toString() + "]";
  }
}
