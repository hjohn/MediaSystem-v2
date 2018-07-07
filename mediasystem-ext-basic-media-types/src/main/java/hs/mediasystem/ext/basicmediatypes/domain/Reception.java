package hs.mediasystem.ext.basicmediatypes.domain;

public class Reception {
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
