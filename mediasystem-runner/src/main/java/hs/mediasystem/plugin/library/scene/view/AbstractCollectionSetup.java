package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.CollectionDetails;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.serie.ProductionPresentation;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;

import javax.inject.Inject;

public abstract class AbstractCollectionSetup<T extends MediaDescriptor, P extends GridViewPresentation<T>> extends AbstractSetup<T, P> {
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private ProductionPresentation.Factory productionPresentationFactory;
  @Inject private ProductionCollectionPresentation.Factory productionCollectPresentationFactory;

  @Override
  protected void configureCellFactory(MediaGridViewCellFactory<T> cellFactory) {
    cellFactory.setTitleBindProvider(item -> item.productionTitle);
    cellFactory.setSideBarTopLeftBindProvider(item -> item.productionYearRange);
    cellFactory.setSideBarCenterBindProvider(item -> new SimpleStringProperty(Optional.ofNullable(item.getData()).filter(Movie.class::isInstance).map(Movie.class::cast).map(Movie::getCollectionDetails).map(CollectionDetails::getDetails).map(Details::getName).orElse("")));
    cellFactory.setImageExtractor(item -> Optional.ofNullable(item.getDetails()).map(Details::getImage).map(imageHandleFactory::fromURI).orElse(null));
    cellFactory.setMediaStatusBindProvider(item -> item.mediaStatus);
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<T>> event, P presentation) {
    if(event.getItem().getData() instanceof ProductionCollection) {
      PresentationLoader.navigate(event, () -> productionCollectPresentationFactory.create(event.getItem().getData().getIdentifier()));
    }
    else {
      PresentationLoader.navigate(event, () -> productionPresentationFactory.create(event.getItem()));
    }
  }
}
