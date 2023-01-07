package hs.mediasystem.plugin.home;

import hs.mediasystem.plugin.cell.AnnotatedImageCellFactory;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.presentation.PresentationLoader;
import hs.mediasystem.util.javafx.base.Nodes;
import hs.mediasystem.util.javafx.control.ActionListView;

import javax.inject.Singleton;

@Singleton
public class NewItemsNodeFactory extends AbstractCarouselNodeFactory implements NodeFactory<NewItemsPresentation> {

  @Override
  public ActionListView<NewItemsPresentation.Item> create(NewItemsPresentation presentation) {
    HorizontalCarousel<NewItemsPresentation.Item> mediaGridView = new HorizontalCarousel<>(
      presentation.getNewItems(),
      e -> PresentationLoader.navigate(e, () -> getRecommendedProductionPresentation(e.getItem().recommendation)),
      new AnnotatedImageCellFactory<NewItemsPresentation.Item>(this::fillRecommendationModel)
    );

    Nodes.safeBindBidirectionalSelectedItemToModel(mediaGridView, presentation.selectedItem);

    return mediaGridView;
  }

  private void fillRecommendationModel(NewItemsPresentation.Item item, AnnotatedImageCellFactory.Model model) {
    fillRecommendationModel(
      item.recommendation,
      item.parentTitle,
      item.title,
      item.subtitle,
      model
    );
  }
}