package hs.mediasystem.plugin.library.scene.grid.contribution;

import hs.mediasystem.plugin.library.scene.base.ContextLayout;
import hs.mediasystem.plugin.library.scene.grid.AbstractSetup;
import hs.mediasystem.plugin.library.scene.grid.contribution.ContributionsPresentationFactory.ContributionsPresentation;
import hs.mediasystem.plugin.library.scene.grid.participation.ParticipationsPresentationFactory;
import hs.mediasystem.presentation.PresentationLoader;
import hs.mediasystem.ui.api.domain.Contribution;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ContributionsSetup extends AbstractSetup<Contribution, ContributionsPresentation> {
  @Inject private ContextLayout contextLayout;
  @Inject private ParticipationsPresentationFactory personParticipationsPresentationFactory;

  @Override
  protected Node createContextPanel(ContributionsPresentation presentation) {
    return contextLayout.create(presentation.work);
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<Contribution> event, ContributionsPresentation presentation) {
    PresentationLoader.navigate(event, () -> personParticipationsPresentationFactory.create(event.getItem().getPerson().getId()));
  }
}
