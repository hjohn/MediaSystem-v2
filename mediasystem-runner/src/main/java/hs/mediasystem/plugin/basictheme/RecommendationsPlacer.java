package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.grid.recommendation.RecommendationsSetup;
import hs.mediasystem.plugin.library.scene.grid.recommendation.RecommendationsPresentationFactory.RecommendationsPresentation;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.ui.api.domain.Work;

import javax.inject.Singleton;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = RecommendationsSetup.class)
public class RecommendationsPlacer extends AbstractPlacer<LibraryPresentation, RecommendationsPresentation, RecommendationsSetup> {

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, RecommendationsPresentation presentation) {
    parentPresentation.backdrop.bind(presentation.selectedItem
      .filter(Work.class::isInstance)
      .map(Work.class::cast)
      .map(Work::getDetails)
      .map(d -> d.getBackdrop().orElse(null))
    );
  }
}
