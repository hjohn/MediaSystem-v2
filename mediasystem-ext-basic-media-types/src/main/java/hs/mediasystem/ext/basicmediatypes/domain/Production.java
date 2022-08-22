package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Represents either a Movie, a TV Movie or an entire TV Series.
 */
public class Production extends Release {
  private final Classification classification;
  private final double popularity;
  private final Set<WorkId> relatedWorks;

  public Production(WorkId id, Details details, Reception reception, Classification classification, double popularity, Set<WorkId> relatedWorks) {
    super(id, details, reception);

    if(classification == null) {
      throw new IllegalArgumentException("classification cannot be null");
    }

    this.popularity = popularity;
    this.classification = classification;
    this.relatedWorks = Set.copyOf(relatedWorks);
  }

  public List<String> getLanguages() {
    return classification.languages();
  }

  public List<String> getGenres() {
    return classification.genres();
  }

  public Classification getClassification() {
    return classification;
  }

  public double getPopularity() {
    return popularity;
  }

  public Set<WorkId> getRelatedWorks() {
    return relatedWorks;
  }

  public Optional<WorkId> getCollectionId() {
    return relatedWorks.stream().filter(i -> i.getType().equals(MediaType.COLLECTION)).findAny();
  }

  @Override
  public String toString() {
    return "Production[" + super.toString() + "]";
  }
}
