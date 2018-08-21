package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.plugin.library.scene.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.RecommendationsPresentation;
import hs.mediasystem.plugin.library.scene.view.RecommendationsSetup;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.runner.ImageHandleFactory;
import hs.mediasystem.util.javafx.Binds;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = RecommendationsSetup.class)
public class RecommendationsPlacer extends AbstractPlacer<LibraryPresentation, RecommendationsPresentation, RecommendationsSetup> {
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, RecommendationsPresentation presentation) {
    parentPresentation.backdrop.bind(Binds.monadic(presentation.selectedItem).map(MediaItem::getProduction).map(Production::getBackdrop).map(imageHandleFactory::fromURI));
  }
}
