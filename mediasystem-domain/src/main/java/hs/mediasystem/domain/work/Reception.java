package hs.mediasystem.domain.work;

import java.util.Comparator;

public record Reception(double rating, long voteCount) {
  public static final Comparator<Reception> RATING_REVERSED = Comparator.comparingDouble(Reception::rating).reversed();
  public static final Reception EMPTY = new Reception(0, 0);
}
