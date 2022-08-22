package hs.mediasystem.ui.api.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record Classification(List<String> keywords, List<String> genres, List<String> languages, Map<String, String> contentRatings, Boolean pornographic, Optional<Stage> stage) {
  public static final Classification DEFAULT = new Classification(List.of(), List.of(), List.of(), Map.of(), null, Optional.empty());
}
