package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.plugin.library.scene.LibraryLocation;

import javafx.scene.Node;

/**
 * Node associated with a Location.
 */
public interface View<P> {
  boolean isApplicable(LibraryLocation location);
  Node create(P presentation);
}
