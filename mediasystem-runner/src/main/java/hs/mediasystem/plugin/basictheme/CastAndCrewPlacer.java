package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.view.CastAndCrewPresentation;
import hs.mediasystem.plugin.library.scene.view.CastAndCrewSetup;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageHandleFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Val;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = CastAndCrewSetup.class)
public class CastAndCrewPlacer extends AbstractPlacer<LibraryPresentation, CastAndCrewPresentation, CastAndCrewSetup> {
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, CastAndCrewPresentation presentation) {
    Val<ImageHandle> val = Val.constant(presentation.mediaItem)
      .map(MediaItem::getProduction)
      .map(Production::getBackdrop)
      .map(imageHandleFactory::fromURI)
      .orElse(Val.constant(presentation.mediaItem)
        .map(MediaItem::getParent)
        .map(MediaItem::getProduction)
        .map(Production::getBackdrop)
        .map(imageHandleFactory::fromURI)
      );

    parentPresentation.backdrop.bind(val);
  }
}
