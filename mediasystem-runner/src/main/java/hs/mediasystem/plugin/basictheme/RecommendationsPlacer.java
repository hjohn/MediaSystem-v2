package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.view.RecommendationsPresentation;
import hs.mediasystem.plugin.library.scene.view.RecommendationsSetup;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.util.ImageHandleFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Val;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = RecommendationsSetup.class)
public class RecommendationsPlacer extends AbstractPlacer<LibraryPresentation, RecommendationsPresentation, RecommendationsSetup> {
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, RecommendationsPresentation presentation) {
    parentPresentation.backdrop.bind(Val.wrap(presentation.selectedItem).map(MediaItem::getDetails).map(d -> d.getBackdrop().orElse(null)).map(imageHandleFactory::fromURI));
  }
}
