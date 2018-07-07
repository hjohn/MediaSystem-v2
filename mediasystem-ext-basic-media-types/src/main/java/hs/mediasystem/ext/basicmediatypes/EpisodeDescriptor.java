package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;

import java.time.Duration;
import java.util.List;

public class EpisodeDescriptor extends AbstractMediaDescriptor {
  private final Production production;
  private final String tagLine;
  private final Duration runtime;
  private final List<String> languages;
  private final List<String> genres;

  public EpisodeDescriptor(ProductionIdentifier serieIdentifier, Production production, String tagLine, Duration runtime, List<String> languages, List<String> genres) {
    this.production = production;
    this.tagLine = tagLine;
    this.runtime = runtime;
    this.languages = languages;
    this.genres = genres;
  }

  public Production getProduction() {
    return production;
  }

  public Duration getRuntime() {
    return runtime;
  }

  public List<String> getLanguages() {
    return languages;
  }

  public List<String> getGenres() {
    return genres;
  }

  public String getTagLine() {
    return tagLine;
  }
}
