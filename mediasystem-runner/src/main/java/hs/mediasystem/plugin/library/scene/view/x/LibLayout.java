package hs.mediasystem.plugin.library.scene.view.x;

import hs.mediasystem.plugin.library.scene.EntityPresentation;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import javax.inject.Singleton;

@Singleton
public class LibLayout extends BaseLayout {


  public LibLayout() {
    super(LibLocation.class);
  }

  // MovieCollectionLocation, subclass of LibraryLocation
  // To generate UI (first time), finds layout that supports MovieCollectionLocation,
  // calls create.
  // - Two routes
  //    1) Top-down, parent is provided to child
  //    2) Bottom-up, child is provided to parent --> issue, AreaPane parents cannot be supported?  There is lookup by CSS id...



  public Fragment create() {
    Node node = new StackPane();

    EntityPresentation entityPresentation = new EntityPresentation();

    return new Fragment(node, entityPresentation);
  }

}
