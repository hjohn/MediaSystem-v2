package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.grid.contribution.ContributionsPresentation;
import hs.mediasystem.plugin.library.scene.grid.contribution.ContributionsSetup;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageHandleFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Val;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = ContributionsSetup.class)
public class ContributionsPlacer extends AbstractPlacer<LibraryPresentation, ContributionsPresentation, ContributionsSetup> {
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, ContributionsPresentation presentation) {
    Val<ImageHandle> val = Val.constant(presentation.work)
      .map(d -> d.getDetails().getBackdrop().orElse(null))
      .map(imageHandleFactory::fromURI)
//      .orElse(Val.constant(presentation.release)
//        .map(MediaItem::getParent)
//        .map(MediaItem::getDetails)
//        .map(d -> d.getBackdrop().orElse(null))
//        .map(imageHandleFactory::fromURI)
//      );
      ;

    parentPresentation.backdrop.bind(val);
  }
}
