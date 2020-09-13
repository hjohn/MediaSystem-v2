package hs.mediasystem.plugin.library.scene.grid.participation;

import hs.mediasystem.plugin.library.scene.base.ContextLayout;
import hs.mediasystem.plugin.library.scene.grid.AbstractSetup;
import hs.mediasystem.plugin.library.scene.grid.participation.ParticipationsPresentationFactory.ParticipationsPresentation;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory;
import hs.mediasystem.presentation.PresentationLoader;
import hs.mediasystem.ui.api.domain.Participation;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ParticipationsSetup extends AbstractSetup<Participation, ParticipationsPresentation> {
  @Inject private ContextLayout contextLayout;
  @Inject private ProductionPresentationFactory productionPresentationFactory;

  @Override
  protected Node createContextPanel(ParticipationsPresentation presentation) {
    return contextLayout.create(presentation.person);
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<Participation> event, ParticipationsPresentation presentation) {
    PresentationLoader.navigate(event, () -> productionPresentationFactory.create(event.getItem().getWork().getId()));
  }
}
