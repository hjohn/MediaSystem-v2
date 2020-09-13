package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.plugin.library.scene.grid.RecommendationsPresentationFactory.RecommendationsPresentation;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory;
import hs.mediasystem.presentation.PresentationLoader;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RecommendationsSetup extends AbstractSetup<Work, RecommendationsPresentation> {
  @Inject private ProductionPresentationFactory productionPresentationFactory;

  @Override
  protected void onItemSelected(ItemSelectedEvent<Work> event, RecommendationsPresentation presentation) {
    PresentationLoader.navigate(event, () -> productionPresentationFactory.create(event.getItem().getId()));
  }
}
