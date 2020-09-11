package hs.mediasystem.ui.api.domain;

import java.util.Comparator;
import java.util.Optional;

public class Sequence {
  public static final Comparator<Sequence> COMPARATOR = Comparator
      .comparingInt((Sequence o) -> o.getType().ordinal())
      .thenComparingInt((Sequence o) -> o.getSeasonNumber().orElse(0))
      .thenComparingInt((Sequence o) -> o.getNumber());

  public enum Type {EPISODE, SPECIAL, EXTRA}

  private final Type type;
  private final int number;
  private final Optional<Integer> seasonNumber;

  public Sequence(Type type, int number, Integer seasonNumber) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(number < 0) {
      throw new IllegalArgumentException("number must not be negative: " + number);
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
