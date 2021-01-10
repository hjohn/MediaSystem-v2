package hs.mediasystem.domain.work;

import java.util.Comparator;

public class Reception {
  public static final Comparator<Reception> RATING_REVERSED = Comparator.comparingDouble(Reception::getRating).reversed();
  public static final Reception EMPTY = new Reception(0, 0);

  private final double rating;
  private final long voteCount;

  public Reception(double rating, long voteCount) {
    this.rating = rating;
    this.voteCount = voteCount;
  }

  public double getRating() {
    return rating;
  }

  public long getVoteCount() {
    return voteCount;
  }
}
