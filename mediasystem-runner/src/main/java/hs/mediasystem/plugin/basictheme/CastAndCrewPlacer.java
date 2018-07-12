package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.plugin.library.scene.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.CastAndCrewPresentation;
import hs.mediasystem.plugin.library.scene.view.CastAndCrewSetup;
import hs.mediasystem.plugin.library.scene.view.SerieCollectionSetup;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.runner.ImageHandleFactory;
import hs.mediasystem.util.javafx.Binds;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = SerieCollectionSetup.class)
public class CastAndCrewPlacer extends AbstractPlacer<LibraryPresentation, CastAndCrewPresentation, CastAndCrewSetup> {
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, CastAndCrewPresentation presentation) {
    parentPresentation.backdrop.bind(Binds.monadic(presentation.mediaItem).map(MediaItem::getProduction).map(Production::getBackdrop).map(imageHandleFactory::fromURI));
  }
}
