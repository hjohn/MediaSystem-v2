package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.view.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.view.GenericCollectionSetup;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.util.ImageHandleFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Val;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = GenericCollectionSetup.class)
public class GenericCollectionPlacer extends AbstractPlacer<LibraryPresentation, GenericCollectionPresentation<MediaDescriptor>, GenericCollectionSetup> {
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, GenericCollectionPresentation<MediaDescriptor> presentation) {
    parentPresentation.backdrop.bind(Val.wrap(presentation.selectedItem).map(MediaItem::getDetails).map(Details::getBackdrop).map(imageHandleFactory::fromURI));
  }
}
