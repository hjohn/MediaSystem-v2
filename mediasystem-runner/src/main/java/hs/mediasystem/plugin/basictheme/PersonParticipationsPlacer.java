package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.plugin.library.scene.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.PersonParticipationsPresentation;
import hs.mediasystem.plugin.library.scene.view.PersonParticipationsSetup;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.runner.ImageHandleFactory;
import hs.mediasystem.util.javafx.Binds;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = PersonParticipationsSetup.class)
public class PersonParticipationsPlacer extends AbstractPlacer<LibraryPresentation, PersonParticipationsPresentation, PersonParticipationsSetup> {
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, PersonParticipationsPresentation presentation) {
    parentPresentation.backdrop.bind(Binds.monadic(presentation.selectedItem).map(MediaItem::getProduction).map(Production::getBackdrop).map(imageHandleFactory::fromURI));
  }
}
