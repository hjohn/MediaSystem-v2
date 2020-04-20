package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.domain.work.Reception;

import java.util.List;
import java.util.Set;

public class Folder extends Production {

  /**
   * Constructs a new instance.
   *
   * @param identifier a {@link ProductionIdentifier}, cannot be null
   * @param details a {@link Details}, cannot be null
   * @param reception a {@link Reception}, can be null
   * @param languages a list of language codes, cannot be null but can be empty
   * @param genres a list of genres, cannot be null but can be empty
   */
  public Folder(ProductionIdentifier identifier, Details details, Reception reception, List<String> languages, List<String> genres) {
    super(identifier, details, reception, languages, genres, 1.0, Set.of());
  }
}
