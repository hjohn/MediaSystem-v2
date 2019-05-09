package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.view.MovieCollectionSetup;
import hs.mediasystem.plugin.movies.videolibbaroption.MovieCollectionPresentation;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.javafx.Binds;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = MovieCollectionSetup.class)
public class MovieCollectionPlacer extends AbstractPlacer<LibraryPresentation, MovieCollectionPresentation, MovieCollectionSetup> {
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, MovieCollectionPresentation presentation) {
    parentPresentation.backdrop.bind(Binds.monadic(presentation.selectedItem).map(MediaItem::getProduction).map(Production::getBackdrop).map(imageHandleFactory::fromURI));
  }
}
