package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.BeanUtils;
import hs.mediasystem.util.Localizable;

public class WorkNotFoundException extends RuntimeException implements Localizable {
  private final WorkId id;

  public WorkNotFoundException(WorkId id) {
    this.id = id;
  }

  private static String toLocalizedItemName(MediaType mediaType) {
    return mediaType.name().toLowerCase();
  }

  @Override
  public String toLocalizedString() {
    return "### " + BeanUtils.capitalize(toLocalizedItemName(id.getType())) + " unavailable\n"
        + "MediaSystem was unable to display an item because it is no longer"
        + "available.\n\n"
        + "#### Technical details\n"
        + "Unable to fetch item with id `" + id + "`";
  }
}
