package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.grid.GenericCollectionPresentationFactory.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.grid.GenericCollectionSetup;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.runner.grouping.WorksGroup;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Work;

import javax.inject.Singleton;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = GenericCollectionSetup.class)
public class GenericCollectionPlacer extends AbstractPlacer<LibraryPresentation, GenericCollectionPresentation<Work, Object>, GenericCollectionSetup> {

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, GenericCollectionPresentation<Work, Object> presentation) {
    parentPresentation.backdrop.bind(presentation.selectedItem
      .map(i -> {
        if(i instanceof Work w) return w.getDetails();
        if(i instanceof WorksGroup wg) return wg.getDetails();
        return null;
      })
      .map(Details::getBackdrop)
      .map(o -> o.orElse(null))
    );
  }
}
