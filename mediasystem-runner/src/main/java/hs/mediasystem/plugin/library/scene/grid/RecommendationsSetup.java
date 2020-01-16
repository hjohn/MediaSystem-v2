package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.plugin.library.scene.overview.ProductionPresentation;
import hs.mediasystem.presentation.PresentationLoader;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RecommendationsSetup extends AbstractSetup<Work, RecommendationsPresentation> {
  @Inject private ProductionPresentation.Factory productionPresentationFactory;

  @Override
  protected void onItemSelected(ItemSelectedEvent<Work> event, RecommendationsPresentation presentation) {
    PresentationLoader.navigate(event, () -> productionPresentationFactory.create(event.getItem().getId()));
  }
}
