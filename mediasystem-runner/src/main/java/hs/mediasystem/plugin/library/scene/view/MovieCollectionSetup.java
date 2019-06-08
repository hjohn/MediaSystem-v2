package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.domain.DetailedMediaDescriptor;
import hs.mediasystem.plugin.movies.videolibbaroption.MovieCollectionPresentation;

import javafx.scene.Node;

import javax.inject.Singleton;

@Singleton
public class MovieCollectionSetup extends AbstractCollectionSetup<DetailedMediaDescriptor, MovieCollectionPresentation> {

  @Override
  public Node create(MovieCollectionPresentation presentation) {
    return createView(presentation);
  }
}
