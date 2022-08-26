package hs.mediasystem.ext.local;

import java.time.LocalDate;
import java.util.List;

public class Description {
  private final String title;
  private final String subtitle;
  private final String description;
  private final String tagLine;
  private final List<String> genres;
  private final LocalDate date;

  public Description(String title, String subtitle, String description, String tagLine, List<String> genres, LocalDate date) {
    this.title = title;
    this.subtitle = subtitle;
    this.description = description;
    this.tagLine = tagLine;
    this.genres = genres;
    this.date = date;
  }

  public String getTitle() {
    return title;
  }

  public String getSubtitle() {
    return subtitle;
  }

  public String getDescription() {
    return description;
  }

  public String getTagLine() {
    return tagLine;
  }

  public List<String> getGenres() {
    return genres;
  }

  public LocalDate getDate() {
    return date;
  }
}
