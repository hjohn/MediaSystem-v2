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
  private final Classification classification;
  private final double popularity;
  private final Set<Identifier> relatedIdentifiers;

  public Production(ProductionIdentifier identifier, Details details, Reception reception, Classification classification, double popularity, Set<Identifier> relatedIdentifiers) {
    super(identifier, details, reception);

    if(classification == null) {
      throw new IllegalArgumentException("classification cannot be null");
    }

    this.popularity = popularity;
    this.classification = classification;
    this.relatedIdentifiers = Set.copyOf(relatedIdentifiers);
  }

  public List<String> getLanguages() {
    return classification.getLanguages();
  }

  public List<String> getGenres() {
    return classification.getGenres();
  }

  public Classification getClassification() {
    return classification;
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
