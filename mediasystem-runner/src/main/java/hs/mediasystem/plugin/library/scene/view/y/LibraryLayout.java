package hs.mediasystem.plugin.library.scene.view.y;

import javafx.scene.Node;

public class LibraryLayout implements Layout<LibraryLocation, View> {

  @Override
  public LibraryView create(View view, LibraryLocation location) {
    return null;
  }

  @Override
  public Class<LibraryLocation> getLocationClass() {
    return LibraryLocation.class;
  }

  public static class LibraryView extends AbstractView {
    public LibraryView(Node node) {
      super(node);
    }
  }
}
