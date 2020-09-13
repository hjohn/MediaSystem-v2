package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.grid.GenericCollectionPresentationFactory.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.grid.GenericCollectionSetup;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.runner.grouping.WorksGroup;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.ImageHandleFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Val;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = GenericCollectionSetup.class)
public class GenericCollectionPlacer extends AbstractPlacer<LibraryPresentation, GenericCollectionPresentation<Work>, GenericCollectionSetup> {
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, GenericCollectionPresentation<Work> presentation) {
    parentPresentation.backdrop.bind(
      Val.wrap(presentation.selectedItem)
        .filter(Work.class::isInstance)
        .map(Work.class::cast)
        .map(Work::getDetails)
        .map(Details::getBackdrop)
        .orElse(Val.wrap(presentation.selectedItem).filter(WorksGroup.class::isInstance).map(WorksGroup.class::cast).map(WorksGroup::getDetails).map(Details::getBackdrop))
        .map(o -> o.orElse(null))
        .map(imageHandleFactory::fromURI)
    );
  }
}
