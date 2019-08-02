package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.ext.basicmediatypes.Identifier;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public class Movie extends Production {
  public enum State {
    PLANNED,
    IN_PRODUCTION,
    RELEASED
  }

  private final Duration runtime;
  private final List<Keyword> keywords;
  private final String tagLine;
  private final State state;
  private final CollectionDetails collectionDetails;

  public Movie(ProductionIdentifier identifier, Details details, Reception reception, Duration runtime, List<String> languages, List<String> genres, List<Keyword> keywords, double popularity, String tagLine, State state, CollectionDetails collectionDetails, Set<Identifier> relatedIdentifiers) {
    super(identifier, details, reception, languages, genres, popularity, relatedIdentifiers);

    if(keywords == null) {
      throw new IllegalArgumentException("keywords cannot be null");
    }

    this.runtime = runtime;
    this.keywords = keywords;
    this.state = state;
    this.tagLine = tagLine;
    this.collectionDetails = collectionDetails;
  }

  public Duration getRuntime() {
    return runtime;
  }

  public List<Keyword> getKeywords() {
    return keywords;
  }

  public State getState() {
    return state;
  }

  public String getTagLine() {
    return tagLine;
  }

  public CollectionDetails getCollectionDetails() {
    return collectionDetails;
  }
}
