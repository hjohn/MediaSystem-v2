package hs.mediasystem.plugin.library.scene;

import java.util.Map;
import java.util.Optional;

public class Sequence {
  public enum Type {SEASON, EPISODE}

  private final Map<Type, Integer> values;

  public static Sequence seasonEpisode(int season, int episode) {
    return new Sequence(Map.of(Type.SEASON, season, Type.EPISODE, episode));
  }

  private Sequence(Map<Type, Integer> values) {
    this.values = values;
  }

  public Optional<Integer> getValue(Type type) {
    return Optional.ofNullable(values.get(type));
  }
}
