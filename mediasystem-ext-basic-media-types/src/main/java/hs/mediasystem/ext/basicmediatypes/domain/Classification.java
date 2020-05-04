package hs.mediasystem.ext.basicmediatypes.domain;

import java.util.List;
import java.util.Map;

public class Classification {
  public static final Classification EMPTY = new Classification(List.of(), List.of(), List.of(), Map.of(), null);

  private final List<String> genres;
  private final List<String> languages;
  private final List<Keyword> keywords;
  private final Map<String, String> contentRatings;
  private final Boolean pornographic;

  public Classification(List<String> genres, List<String> languages, List<Keyword> keywords, Map<String, String> contentRatings, Boolean pornographic) {
    if(genres == null) {
      throw new IllegalArgumentException("genres cannot be null");
    }
    if(languages == null) {
      throw new IllegalArgumentException("languages cannot be null");
    }
    if(keywords == null) {
      throw new IllegalArgumentException("keywords cannot be null");
    }
    if(contentRatings == null) {
      throw new IllegalArgumentException("contentRatings cannot be null");
    }

    this.genres = List.copyOf(genres);
    this.languages = List.copyOf(languages);
    this.keywords = List.copyOf(keywords);
    this.contentRatings = Map.copyOf(contentRatings);
    this.pornographic = pornographic;
  }

  public List<String> getGenres() {
    return genres;
  }

  public List<String> getLanguages() {
    return languages;
  }

  public List<Keyword> getKeywords() {
    return keywords;
  }

  public Map<String, String> getContentRatings() {
    return contentRatings;
  }

  public Boolean getPornographic() {
    return pornographic;
  }
}
