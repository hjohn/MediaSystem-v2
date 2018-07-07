package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.plugin.library.scene.EntityPresentation;
import hs.mediasystem.plugin.library.scene.LibraryLocation;

import javafx.scene.Node;

public interface Setup<P> {
  P createPresentation(EntityPresentation entityPresentation);
  boolean isApplicable(LibraryLocation location);
  //void configurePanes(LibraryLocation location, AreaPane2<Area> areaPane, EntityPresentation entityPresentation);
  Node createView(P presentation);
}
