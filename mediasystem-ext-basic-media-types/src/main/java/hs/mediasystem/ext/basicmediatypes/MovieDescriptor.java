package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.ext.basicmediatypes.domain.Production;

public class MovieDescriptor extends AbstractProductionDescriptor {
  private final String tagLine;

  public MovieDescriptor(Production production, String tagLine) {
    super(production);

    this.tagLine = tagLine;
  }

  public String getTagLine() {
    return tagLine;
  }
}
