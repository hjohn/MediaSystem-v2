package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.plugin.library.scene.EntityPresentation;
import hs.mediasystem.plugin.library.scene.LibraryLocation;
import hs.mediasystem.runner.LocationPresentation;

public class DetailPresentation extends LocationPresentation<LibraryLocation> {
  private final EntityPresentation entityPresentation;

  public DetailPresentation(EntityPresentation entityPresentation) {
    this.entityPresentation = entityPresentation;
  }

  public EntityPresentation getEntityPresentation() {
    return entityPresentation;
  }
}
