package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.domain.Serie;

import javafx.scene.Node;

import javax.inject.Singleton;

@Singleton
public class SerieCollectionSetup extends AbstractCollectionSetup<Serie, SerieCollectionPresentation> {

  @Override
  public Node create(SerieCollectionPresentation presentation) {
    return createView(presentation);
  }
}
