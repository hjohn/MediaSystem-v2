package hs.mediasystem.ext.basicmediatypes.domain;

public class Keyword {
  private final Identifier identifier;
  private final String text;

  public Keyword(Identifier identifier, String text) {
    if(identifier == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    if(text == null) {
      throw new IllegalArgumentException("text cannot be null");
    }

    this.identifier = identifier;
    this.text = text;
  }

  public Identifier getIdentifier() {
    return identifier;
  }

  public String getText() {
    return text;
  }
}
