package hs.mediasystem.ext.basicmediatypes.domain;

import java.util.List;
import java.util.Map;

public record Classification(List<String> genres, List<String> languages, List<Keyword> keywords, Map<String, String> contentRatings, Boolean pornographic) {
  public static final Classification EMPTY = new Classification(List.of(), List.of(), List.of(), Map.of(), null);

  public Classification {
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
  }
}
