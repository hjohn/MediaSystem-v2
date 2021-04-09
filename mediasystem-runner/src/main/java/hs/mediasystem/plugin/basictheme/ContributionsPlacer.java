package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.grid.contribution.ContributionsPresentationFactory.ContributionsPresentation;
import hs.mediasystem.plugin.library.scene.grid.contribution.ContributionsSetup;
import hs.mediasystem.presentation.PlacerQualifier;

import javax.inject.Singleton;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = ContributionsSetup.class)
public class ContributionsPlacer extends AbstractPlacer<LibraryPresentation, ContributionsPresentation, ContributionsSetup> {

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, ContributionsPresentation presentation) {
    parentPresentation.backdrop.bind(
      presentation.work
        .map(d -> d.getDetails().getBackdrop().orElse(null))
    );
  }
}
