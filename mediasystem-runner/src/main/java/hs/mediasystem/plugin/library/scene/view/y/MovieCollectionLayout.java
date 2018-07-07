package hs.mediasystem.plugin.library.scene.view.y;

import hs.mediasystem.plugin.library.scene.view.y.LibraryLayout.LibraryView;

public class MovieCollectionLayout implements Layout<MovieCollectionLocation, LibraryView> {

  @Override
  public View create(LibraryView view, MovieCollectionLocation location) {
    return null;
  }

  @Override
  public Class<MovieCollectionLocation> getLocationClass() {
    return MovieCollectionLocation.class;
  }
}
