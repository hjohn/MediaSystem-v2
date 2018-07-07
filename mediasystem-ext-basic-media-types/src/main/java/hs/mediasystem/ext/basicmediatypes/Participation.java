package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Role;

/**
 * A participation in a {@link Production}.
 */
public class Participation {
  public enum Type {MOVIE, TV}

  private final Type type;
  private final Production production;
  private final Integer episodeCount;
  private final Reception reception;
  private final Role role;

  public static Participation inTV(Production production, int episodeCount, Reception reception, Role role) {
    return new Participation(Type.TV, production, episodeCount, reception, role);
  }

  public static Participation inMovie(Production production, Reception reception, Role role) {
    return new Participation(Type.MOVIE, production, null, reception, role);
  }

  private Participation(Type type, Production production, Integer episodeCount, Reception reception, Role role) {
    this.type = type;
    this.production = production;
    this.episodeCount = episodeCount;
    this.reception = reception;
    this.role = role;
  }

  public Type getType() {
    return type;
  }

  public Production getProduction() {
    return production;
  }

  public Integer getEpisodeCount() {
    return episodeCount;
  }

  public Reception getReception() {
    return reception;
  }

  public Role getRole() {
    return role;
  }
}
