package hs.mediasystem.ui.api.domain;

import java.util.Optional;

public class Sequence {
  public enum Type {SPECIAL, EXTRA, EPISODE}

  private final Type type;
  private final int number;
  private final Optional<Integer> seasonNumber;

  public Sequence(Type type, int number, Integer seasonNumber) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(number <= 0) {
      throw new IllegalArgumentException("number must be positive: " + number);
    }
    if(seasonNumber != null && seasonNumber <= 0) {
      throw new IllegalArgumentException("seasonNumber must be positive: " + seasonNumber);
    }

    this.type = type;
    this.number = number;
    this.seasonNumber = Optional.ofNullable(seasonNumber);
  }

  public Type getType() {
    return type;
  }

  public int getNumber() {
    return number;
  }

  public Optional<Integer> getSeasonNumber() {
    return seasonNumber;
  }
}
