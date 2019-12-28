package hs.mediasystem.plugin.library.scene.grid.contribution;

import hs.mediasystem.db.SettingsSourceFactory;
import hs.mediasystem.db.SettingsSourceFactory.SettingsSource;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Contribution;
import hs.mediasystem.ext.basicmediatypes.domain.stream.PersonId;
import hs.mediasystem.plugin.library.scene.base.ContextLayout;
import hs.mediasystem.plugin.library.scene.grid.AbstractSetup;
import hs.mediasystem.plugin.library.scene.grid.participation.ParticipationsPresentation;
import hs.mediasystem.presentation.PresentationLoader;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ContributionsSetup extends AbstractSetup<Contribution, ContributionsPresentation> {
  @Inject private ContextLayout contextLayout;
  @Inject private ParticipationsPresentation.Factory personParticipationsPresentationFactory;
  @Inject private SettingsSourceFactory settingsSourceFactory;

  @Override
  protected Node createContextPanel(ContributionsPresentation presentation) {
    return contextLayout.create(presentation.work);
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<Contribution> event, ContributionsPresentation presentation) {
    PresentationLoader.navigate(event, () -> personParticipationsPresentationFactory.create(new PersonId(event.getItem().getPerson().getIdentifier())));
  }

  @Override
  protected SettingsSource getSettingsSource(ContributionsPresentation presentation) {
    return settingsSourceFactory.of(SYSTEM_PREFIX + "CastAndCrew");
  }
}
