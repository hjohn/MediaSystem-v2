package hs.mediasystem.ui.api.domain;

import java.util.Comparator;
import java.util.Optional;

public record Sequence(Type type, int number, Optional<Integer> seasonNumber) {
  public static final Comparator<Sequence> COMPARATOR = Comparator
    .comparingInt((Sequence o) -> o.type().ordinal())
    .thenComparingInt((Sequence o) -> o.seasonNumber().orElse(0))
    .thenComparingInt((Sequence o) -> o.number());

  public enum Type {EPISODE, SPECIAL, EXTRA}

  public Sequence {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(number < 0) {
      throw new IllegalArgumentException("number must not be negative: " + number);
    }
    if(seasonNumber == null) {
      throw new IllegalArgumentException("seasonNumber cannot be null");
    }

    seasonNumber.ifPresent(sn -> {
      if(sn <= 0) {
        throw new IllegalArgumentException("seasonNumber must be positive: " + seasonNumber);
      }
    });
  }
}
