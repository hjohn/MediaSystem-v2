package hs.mediasystem.api.datasource.domain;

import hs.mediasystem.domain.work.KeywordId;

public class Keyword {
  private final KeywordId id;
  private final String text;

  public Keyword(KeywordId id, String text) {
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if(text == null) {
      throw new IllegalArgumentException("text cannot be null");
    }

    this.id = id;
    this.text = text;
  }

  public KeywordId getId() {
    return id;
  }

  public String getText() {
    return text;
  }
}
