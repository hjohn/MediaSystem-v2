package hs.mediasystem.plugin.library.scene.grid.participation;

import hs.mediasystem.domain.work.PersonId;
import hs.mediasystem.util.Localizable;

public class PersonNotFoundException extends RuntimeException implements Localizable {
  private final PersonId id;

  public PersonNotFoundException(PersonId id) {
    this.id = id;
  }

  @Override
  public String toLocalizedString() {
    return "### Person unavailable\n"
        + "MediaSystem was unable to display an item because it is no longer"
        + "available.\n\n"
        + "#### Technical details\n"
        + "Unable to fetch item with id `" + id + "`";
  }
}
