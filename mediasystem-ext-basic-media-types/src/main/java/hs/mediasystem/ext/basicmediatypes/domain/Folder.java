package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.domain.work.Reception;

import java.util.Set;

public class Folder extends Production {

  /**
   * Constructs a new instance.
   *
   * @param identifier a {@link ProductionIdentifier}, cannot be null
   * @param details a {@link Details}, cannot be null
   * @param reception a {@link Reception}, can be null
   * @param classification a {@link Classification}, cannot be null
   */
  public Folder(ProductionIdentifier identifier, Details details, Reception reception, Classification classification) {
    super(identifier, details, reception, classification, 1.0, Set.of());
  }
}
