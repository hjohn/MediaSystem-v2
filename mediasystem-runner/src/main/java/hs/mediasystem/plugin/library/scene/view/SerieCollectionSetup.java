package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.scanner.api.MediaType;

import javafx.scene.Node;

import javax.inject.Singleton;

@Singleton
public class SerieCollectionSetup extends AbstractCollectionSetup<Serie, SerieCollectionPresentation> {
  private static final MediaType SERIE = MediaType.of("SERIE");

  public SerieCollectionSetup() {
    super(SERIE);
  }

  @Override
  public Node create(SerieCollectionPresentation presentation) {
    return createView(presentation);
  }
}
