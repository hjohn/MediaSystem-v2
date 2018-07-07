package hs.mediasystem.plugin.library.scene.view.z;

import javafx.scene.layout.StackPane;

public class ViewPort extends StackPane {

  public ViewPort(BasicTheme theme, Class<? extends NodeFactory<?>> nodeFactoryCls, ParentPresentation presentation) {
    presentation.childPresentationProperty().addListener((obs, old, current) -> {
      @SuppressWarnings("unchecked")
      NodeFactory<Object> childNodeFactory = (NodeFactory<Object>)theme.findNodeFactory(current.getClass());

      Placer placer = theme.findPlacer(nodeFactoryCls, childNodeFactory.getClass());

      getChildren().add(placer.place(childNodeFactory.create(presentation)));
    });
  }
}
