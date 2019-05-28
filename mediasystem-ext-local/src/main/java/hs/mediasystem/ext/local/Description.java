package hs.mediasystem.ext.local;

import java.time.LocalDate;
import java.util.List;

public class Description {
  private final String title;
  private final String description;
  private final List<String> genres;
  private final LocalDate date;

  public Description(String title, String description, List<String> genres, LocalDate date) {
    this.title = title;
    this.description = description;
    this.genres = genres;
    this.date = date;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getGenres() {
    return genres;
  }

  public LocalDate getDate() {
    return date;
  }
}
