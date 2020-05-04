package hs.mediasystem.ui.api.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Classification {
  public static final Classification DEFAULT = new Classification(List.of(), List.of(), List.of(), Map.of(), null, null);

  private final List<String> keywords;
  private final List<String> genres;
  private final List<String> languages;
  private final Map<String, String> contentRatings;
  private final Boolean pornographic;
  private final Optional<Stage> stage;

  public Classification(List<String> keywords, List<String> genres, List<String> languages, Map<String, String> contentRatings, Boolean pornographic, Stage stage) {
    this.keywords = keywords;
    this.genres = genres;
    this.languages = languages;
    this.contentRatings = contentRatings;
    this.pornographic = pornographic;
    this.stage = Optional.ofNullable(stage);
  }

  public List<String> getKeywords() {
    return keywords;
  }

  public List<String> getGenres() {
    return genres;
  }

  public Optional<Stage> getStage() {
    return stage;
  }

  public List<String> getLanguages() {
    return languages;
  }

  public Map<String, String> getContentRatings() {
    return contentRatings;
  }

  public Boolean getPornographic() {
    return pornographic;
  }
}
