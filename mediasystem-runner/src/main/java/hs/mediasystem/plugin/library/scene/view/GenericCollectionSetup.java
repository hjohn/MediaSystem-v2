package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.serie.ProductionPresentation;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import java.util.Optional;

import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GenericCollectionSetup extends AbstractSetup<MediaDescriptor, GenericCollectionPresentation<MediaDescriptor>> {
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private ProductionPresentation.Factory productionPresentationFactory;
  @Inject private ProductionCollectionFactory productionCollectionSupplier;
  @Inject private ContextLayout contextLayout;

  @Override
  protected void configureCellFactory(MediaGridViewCellFactory<MediaDescriptor> cellFactory) {
    cellFactory.setTitleBindProvider(item -> item.productionTitle);
    cellFactory.setSideBarTopLeftBindProvider(item -> item.productionYearRange);
    cellFactory.setSideBarCenterBindProvider(item -> item.collectionTitle);
    cellFactory.setImageExtractor(item -> Optional.ofNullable(item.getDetails()).map(Details::getImage).map(imageHandleFactory::fromURI).orElse(null));
    cellFactory.setMediaStatusBindProvider(item -> item.mediaStatus);
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<MediaDescriptor>> event, GenericCollectionPresentation<MediaDescriptor> presentation) {
    if(event.getItem().getData() instanceof ProductionCollection) {
      PresentationLoader.navigate(event, () -> productionCollectionSupplier.create(event.getItem().getData().getIdentifier()));
    }
    else {
      PresentationLoader.navigate(event, () -> productionPresentationFactory.create(event.getItem()));
    }
  }

  @Override
  protected Node createContextPanel(GenericCollectionPresentation<MediaDescriptor> presentation) {
    return contextLayout.create(presentation.parentDescriptor);
  }
}
