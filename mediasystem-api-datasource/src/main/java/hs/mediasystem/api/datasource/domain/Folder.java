package hs.mediasystem.api.datasource.domain;

import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;

import java.util.Set;

public class Folder extends Production {

  /**
   * Constructs a new instance.
   *
   * @param id a {@link WorkId}, cannot be null
   * @param details a {@link Details}, cannot be null
   * @param reception a {@link Reception}, can be null
   * @param classification a {@link Classification}, cannot be null
   */
  public Folder(WorkId id, Details details, Reception reception, Classification classification) {
    super(id, details, reception, null, null, classification, 1.0, Set.of());
  }
}
