package hs.mediasystem.plugin.home;

import hs.mediasystem.domain.work.Collection;
import hs.mediasystem.plugin.cell.AnnotatedImageCellFactory;
import hs.mediasystem.runner.presentation.PresentationLoader;
import hs.mediasystem.util.image.ImageHandleFactory;
import hs.mediasystem.util.javafx.base.Nodes;
import hs.mediasystem.util.javafx.control.ActionListView;

import javax.inject.Inject;

public class CollectionsNodeFactory {
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private CollectionPresentationProvider collectionPresentationProvider;

  public ActionListView<Collection> create(CollectionsPresentation presentation) {
    HorizontalCarousel<Collection> mediaGridView = new HorizontalCarousel<>(
      presentation.collections,
      e -> PresentationLoader.navigate(e, () -> collectionPresentationProvider.createPresentation(e.getItem().definition().type(), e.getItem().definition().tag())),
      new AnnotatedImageCellFactory<>(this::fillCollectionModel)
    );

    Nodes.safeBindBidirectionalSelectedItemToModel(mediaGridView, presentation.selectedItem);

    return mediaGridView;
  }

  private void fillCollectionModel(Collection collection, AnnotatedImageCellFactory.Model model) {
    model.parentTitle.set(null);
    model.title.set(collection.title());
    model.subtitle.set(null);
    model.sequence.set(null);
    model.imageHandle.set(collection.cover().map(imageHandleFactory::fromURI).orElse(null));
    model.watchedFraction.set(-1);
    model.age.set(null);
  }
}
