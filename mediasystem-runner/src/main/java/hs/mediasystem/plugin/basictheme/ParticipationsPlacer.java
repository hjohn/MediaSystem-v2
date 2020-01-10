package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.grid.participation.ParticipationsPresentation;
import hs.mediasystem.plugin.library.scene.grid.participation.ParticipationsSetup;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.ui.api.domain.Participation;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.ImageHandleFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Val;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = ParticipationsSetup.class)
public class ParticipationsPlacer extends AbstractPlacer<LibraryPresentation, ParticipationsPresentation, ParticipationsSetup> {
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, ParticipationsPresentation presentation) {
    parentPresentation.backdrop.bind(
      Val.wrap(presentation.selectedItem)
        .filter(Participation.class::isInstance)
        .map(Participation.class::cast)
        .map(Participation::getWork)
        .map(Work::getDetails)
        .map(d -> d.getBackdrop().orElse(null))
        .map(imageHandleFactory::fromURI)
    );
  }
}
