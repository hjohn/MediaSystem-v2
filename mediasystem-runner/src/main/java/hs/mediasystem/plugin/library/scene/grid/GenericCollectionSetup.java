package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.plugin.library.scene.grid.GenericCollectionPresentationFactory.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory;
import hs.mediasystem.presentation.PresentationLoader;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GenericCollectionSetup extends AbstractSetup<Work, GenericCollectionPresentation<Work>> {
  @Inject private ProductionPresentationFactory productionPresentationFactory;

  @Override
  protected void onItemSelected(ItemSelectedEvent<Work> event, GenericCollectionPresentation<Work> presentation) {
    PresentationLoader.navigate(event, () -> productionPresentationFactory.create(event.getItem().getId()));
  }
}
