package hs.mediasystem.ui.api.domain;

import java.util.List;

public class Classification {
  public static final Classification DEFAULT = new Classification(List.of(), List.of(), Stage.RELEASED);

  private final List<String> keywords;
  private final List<String> genres;
  private final Stage stage;

  public Classification(List<String> keywords, List<String> genres, Stage stage) {
    this.keywords = keywords;
    this.genres = genres;
    this.stage = stage;
  }

  public List<String> getKeywords() {
    return keywords;
  }

  public List<String> getGenres() {
    return genres;
  }

  public Stage getStage() {
    return stage;
  }
}
