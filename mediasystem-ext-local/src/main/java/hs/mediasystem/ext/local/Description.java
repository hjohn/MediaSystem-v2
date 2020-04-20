package hs.mediasystem.ext.local;

import hs.mediasystem.util.ImageURI;

import java.time.LocalDate;
import java.util.List;

public class Description {
  private final String title;
  private final String subtitle;
  private final String description;
  private final List<String> genres;
  private final LocalDate date;
  private final ImageURI cover;
  private final ImageURI backdrop;

  public Description(String title, String subtitle, String description, List<String> genres, LocalDate date, ImageURI cover, ImageURI backdrop) {
    this.title = title;
    this.subtitle = subtitle;
    this.description = description;
    this.genres = genres;
    this.date = date;
    this.cover = cover;
    this.backdrop = backdrop;
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

  public List<String> getGenres() {
    return genres;
  }

  public LocalDate getDate() {
    return date;
  }

  public ImageURI getCover() {
    return cover;
  }

  public ImageURI getBackdrop() {
    return backdrop;
  }
}
