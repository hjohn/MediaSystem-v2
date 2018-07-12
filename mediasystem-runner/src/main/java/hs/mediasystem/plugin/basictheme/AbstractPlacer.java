package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.presentation.ParentPresentation;
import hs.mediasystem.presentation.Placer;
import hs.mediasystem.presentation.Presentation;

import javafx.scene.Node;

import javax.inject.Inject;

public abstract class AbstractPlacer<P extends ParentPresentation, C extends Presentation, F extends NodeFactory<C>> implements Placer<P, C> {
  @Inject private F nodeFactory;

  @Override
  public Node place(P parentPresentation, C presentation) {
    linkPresentations(parentPresentation, presentation);

    return nodeFactory.create(presentation);
  }

  protected abstract void linkPresentations(P parentPresentation, C presentation);
}
