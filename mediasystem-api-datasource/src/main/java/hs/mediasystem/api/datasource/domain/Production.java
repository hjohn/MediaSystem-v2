package hs.mediasystem.api.datasource.domain;

import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;

import java.util.List;
import java.util.Optional;

/**
 * Represents either a Movie, a TV Movie or an entire TV Series.
 */
public class Production extends Release {
  private final Optional<String> tagLine;
  private final Classification classification;
  private final double popularity;

  public Production(WorkId id, Details details, Reception reception, Parent parent, String tagLine, Classification classification, double popularity) {
    super(id, details, reception, parent);

    if(classification == null) {
      throw new IllegalArgumentException("classification cannot be null");
    }

    this.tagLine = Optional.ofNullable(tagLine);
    this.popularity = popularity;
    this.classification = classification;
  }

  public Optional<String> getTagLine() {
    return tagLine;
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

  @Override
  public String toString() {
    return "Production[" + super.toString() + "]";
  }
}
