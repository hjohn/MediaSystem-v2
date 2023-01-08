package hs.mediasystem.api.datasource.domain.stream;

import hs.mediasystem.api.datasource.domain.Role;

public class Participation {
  private final Role role;
  private final Work work;
  private final int episodeCount;
  private final double popularity;

  public Participation(Role role, Work work, int episodeCount, double popularity) {
    this.role = role;
    this.work = work;
    this.episodeCount = episodeCount;
    this.popularity = popularity;
  }

  public Role getRole() {
    return role;
  }

  public Work getWork() {
    return work;
  }

  public int getEpisodeCount() {
    return episodeCount;
  }

  public double getPopularity() {
    return popularity;
  }
}
