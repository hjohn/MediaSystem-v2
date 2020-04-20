package hs.mediasystem.ui.api.domain;

import java.util.List;
import java.util.Optional;

public class Classification {
  public static final Classification DEFAULT = new Classification(List.of(), List.of(), null);

  private final List<String> keywords;
  private final List<String> genres;
  private final Optional<Stage> stage;

  public Classification(List<String> keywords, List<String> genres, Stage stage) {
    this.keywords = keywords;
    this.genres = genres;
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
}
