package hs.mediasystem.api.datasource.domain;

import hs.mediasystem.domain.work.RoleId;

public class ProductionRole {
  private final Production production;
  private final Role role;
  private final Integer episodeCount;
  private final double popularity;

  public ProductionRole(Production production, Role role, Integer episodeCount, double popularity) {
    if(production == null) {
      throw new IllegalArgumentException("production cannot be null");
    }
    if(role == null) {
      throw new IllegalArgumentException("role cannot be null");
    }

    this.production = production;
    this.role = role;
    this.episodeCount = episodeCount;
    this.popularity = popularity;
  }

  public RoleId getId() {
    return role.getId();
  }

  public Production getProduction() {
    return production;
  }

  public Role getRole() {
    return role;
  }

  public Integer getEpisodeCount() {
    return episodeCount;
  }

  public double getPopularity() {
    return popularity;
  }

  public Details getDetails() {
    return production.getDetails();
  }
}
