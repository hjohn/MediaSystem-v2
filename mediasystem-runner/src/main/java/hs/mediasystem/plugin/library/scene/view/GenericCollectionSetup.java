package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.serie.ProductionPresentation;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GenericCollectionSetup extends AbstractSetup<MediaDescriptor, GenericCollectionPresentation<MediaDescriptor>> {
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private ProductionPresentation.Factory productionPresentationFactory;

  @Override
  protected void configureCellFactory(MediaGridViewCellFactory<MediaItem<MediaDescriptor>> cellFactory) {
    cellFactory.setTitleBindProvider(item -> item.productionTitle);
    cellFactory.setSideBarTopLeftBindProvider(item -> item.productionYearRange);
    cellFactory.setSideBarCenterBindProvider(item -> item.collectionTitle);
    cellFactory.setImageExtractor(item -> item.getDetails().getImage().map(imageHandleFactory::fromURI).orElse(null));
    cellFactory.setMediaStatusBindProvider(item -> item.mediaStatus);
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<MediaDescriptor>> event, GenericCollectionPresentation<MediaDescriptor> presentation) {
    PresentationLoader.navigate(event, () -> {
      @SuppressWarnings("unchecked")
      MediaItem<Production> mediaItem = (MediaItem<Production>)(MediaItem<?>)event.getItem();

      return productionPresentationFactory.create(mediaItem);
    });
  }
}
