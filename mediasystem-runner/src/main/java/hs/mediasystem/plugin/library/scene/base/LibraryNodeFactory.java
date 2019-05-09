package hs.mediasystem.plugin.library.scene.base;

import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.presentation.ViewPortFactory;
import hs.mediasystem.runner.util.LessLoader;

import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LibraryNodeFactory implements NodeFactory<LibraryPresentation> {
  @Inject private ViewPortFactory viewPortFactory;

  @Override
  public Node create(LibraryPresentation presentation) {
    EntityView node = new EntityView(viewPortFactory, presentation);

    node.backgroundPane.backdropProperty().bindBidirectional(presentation.backdrop);
    node.getStylesheets().add(LessLoader.compile(getClass().getResource("styles.less")).toExternalForm());

    return node;
  }
}
