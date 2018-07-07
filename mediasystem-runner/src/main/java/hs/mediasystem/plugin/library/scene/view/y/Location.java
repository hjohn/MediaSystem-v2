package hs.mediasystem.plugin.library.scene.view.y;

public class Location<L> {
  private final L parentLocation;

  public Location(L parentLocation) {
    this.parentLocation = parentLocation;
  }

  public L getParent() {
    return parentLocation;
  }
}
