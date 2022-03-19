package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.grid.participation.ConsolidatedParticipation;
import hs.mediasystem.plugin.library.scene.grid.participation.ParticipationsPresentationFactory.ParticipationsPresentation;
import hs.mediasystem.plugin.library.scene.grid.participation.ParticipationsSetup;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.ui.api.domain.Work;

import javax.inject.Singleton;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = ParticipationsSetup.class)
public class ParticipationsPlacer extends AbstractPlacer<LibraryPresentation, ParticipationsPresentation, ParticipationsSetup> {

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, ParticipationsPresentation presentation) {
    parentPresentation.backdrop.bind(presentation.selectedItem
      .filter(ConsolidatedParticipation.class::isInstance)
      .map(ConsolidatedParticipation.class::cast)
      .map(ConsolidatedParticipation::getWork)
      .map(Work::getDetails)
      .map(d -> d.getBackdrop().orElse(null))
    );
  }
}
