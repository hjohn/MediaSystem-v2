package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.plugin.library.scene.LibraryPresentation;

import javafx.scene.Node;

public interface Setup<P> {
  P createPresentation(LibraryPresentation entityPresentation);
  Node createView(P presentation);
}
