package hs.mediasystem.plugin.library.scene.grid.contribution;

import hs.mediasystem.plugin.cell.MediaGridViewCellFactory.Model;
import hs.mediasystem.plugin.library.scene.base.ContextLayout;
import hs.mediasystem.plugin.library.scene.grid.AbstractSetup;
import hs.mediasystem.plugin.library.scene.grid.contribution.ContributionsPresentationFactory.ContributionsPresentation;
import hs.mediasystem.plugin.library.scene.grid.participation.ParticipationsPresentationFactory;
import hs.mediasystem.presentation.PresentationLoader;
import hs.mediasystem.ui.api.domain.Contribution;
import hs.mediasystem.ui.api.domain.Role;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ContributionsSetup extends AbstractSetup<Contribution, Contribution, ContributionsPresentation> {
  @Inject private ContextLayout contextLayout;
  @Inject private ParticipationsPresentationFactory personParticipationsPresentationFactory;

  @Override
  protected Node createContextPanel(ContributionsPresentation presentation) {
    return contextLayout.create(presentation.work.get());
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<Contribution> event, ContributionsPresentation presentation) {
    PresentationLoader.navigate(event, () -> personParticipationsPresentationFactory.create(event.getItem().person().getId()));
  }

  @Override
  protected Node createPreviewPanel(Contribution item) {
    return contextLayout.create(item.person());
  }

  @Override
  protected void fillModel(Contribution item, Model model) {
    model.title.set(item.person().getName());
    model.imageHandle.set(item.person().getCover().orElse(null));

    Role role = item.role();

    model.subtitle.set(
      role.character() != null && !role.character().isEmpty() ? "as " + role.character()
        : role.job() != null && !role.job().isEmpty() ? role.job()
        : ""
    );
  }
}
