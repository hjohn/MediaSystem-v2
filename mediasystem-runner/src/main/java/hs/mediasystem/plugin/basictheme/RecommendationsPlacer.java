package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.grid.RecommendationsPresentation;
import hs.mediasystem.plugin.library.scene.grid.RecommendationsSetup;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.ui.api.domain.Work;
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
    parentPresentation.backdrop.bind(
      Val.wrap(presentation.selectedItem)
        .filter(Work.class::isInstance)
        .map(Work.class::cast)
        .map(Work::getDetails)
        .map(d -> d.getBackdrop().orElse(null))
        .map(imageHandleFactory::fromURI)
    );
  }
}
