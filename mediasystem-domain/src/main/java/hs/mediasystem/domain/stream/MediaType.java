package hs.mediasystem.domain.stream;

import java.util.EnumSet;
import java.util.Set;

public enum MediaType {

  MOVIE(Trait.PLAYABLE, Trait.HAS_CAST_AND_CREW),
  SERIE(Trait.SERIE, Trait.HAS_CAST_AND_CREW),
  EPISODE(Trait.PLAYABLE, Trait.HAS_CAST_AND_CREW, Trait.COMPONENT),
  SEASON(Trait.SERIE, Trait.COMPONENT),

  FOLDER(),
  FILE(Trait.PLAYABLE),

  COLLECTION();

  private enum Trait {

    /**
     * Consists of video and/or audio.
     */
    PLAYABLE,

    /**
     * Categorized work that may have cast and crew information.
     */
    HAS_CAST_AND_CREW,

    /**
     * Sequence of parts which belong together, like a serie consisting of seasons and episodes.
     */
    SERIE,

    /**
     * Part of a greater whole that cannot stand on its own, like an episode or season.
     */
    COMPONENT
  }

  private final Set<Trait> traits;

  private MediaType(Trait... traits) {
    this.traits = traits.length > 0 ? EnumSet.of(traits[0], traits) : EnumSet.noneOf(Trait.class);
  }

  public boolean isComponent() {
    return traits.contains(Trait.COMPONENT);
  }

  public boolean isPlayable() {
    return traits.contains(Trait.PLAYABLE);
  }

  public boolean isSerie() {
    return traits.contains(Trait.SERIE);
  }

  @Override
  public String toString() {
    return name();
  }

}
