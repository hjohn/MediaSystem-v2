package hs.mediasystem.plugin.library.scene.view.z;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class RootNodeFactory implements NodeFactory<RootPresentation> {

  @Override
  public Node create(RootPresentation presentation) {
    return new StackPane(new ViewPort(new BasicTheme(), RootNodeFactory.class, presentation));
  }

}
