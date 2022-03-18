package hs.mediasystem.plugin.basictheme;

import hs.jfx.eventstream.core.Values;
import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.grid.FolderPresentationFactory.FolderPresentation;
import hs.mediasystem.plugin.library.scene.grid.FolderSetup;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.ImageHandle;

import javax.inject.Singleton;

@Singleton
@PlacerQualifier(parent = LibraryNodeFactory.class, child = FolderSetup.class)
public class FolderViewPlacer extends AbstractPlacer<LibraryPresentation, FolderPresentation, FolderSetup> {

  @Override
  protected void linkPresentations(LibraryPresentation parentPresentation, FolderPresentation presentation) {
    parentPresentation.backdrop.bind(
      Values.of(presentation.selectedItem)
        .map(this::toImageHandle)
        .or(() -> Values.of(presentation.contextItem)
          .map(this::toImageHandle)
        )
        .toBinding()
    );
  }

  private ImageHandle toImageHandle(Object obj) {
    return obj instanceof Work w ? w.getDetails().getBackdrop().orElse(null) : null;
  }
}
