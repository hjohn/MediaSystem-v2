package hs.mediasystem.plugin.home;

import hs.mediasystem.plugin.cell.AnnotatedImageCellFactory;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.runner.presentation.PresentationLoader;
import hs.mediasystem.ui.api.domain.Context;
import hs.mediasystem.ui.api.domain.Recommendation;
import hs.mediasystem.util.javafx.base.Nodes;
import hs.mediasystem.util.javafx.control.ActionListView;

import java.time.LocalDate;

import javax.inject.Singleton;

@Singleton
public class RecommendationsNodeFactory extends AbstractCarouselNodeFactory implements NodeFactory<RecommendationsPresentation> {

  @Override
  public ActionListView<Recommendation> create(RecommendationsPresentation presentation) {
    ActionListView<Recommendation> mediaGridView = new HorizontalCarousel<>(
      presentation.getRecommendations(),
      e -> PresentationLoader.navigate(e, () -> getRecommendedProductionPresentation(e.getItem())),
      new AnnotatedImageCellFactory<Recommendation>(this::fillRecommendationModel)
    );

    Nodes.safeBindBidirectionalSelectedItemToModel(mediaGridView, presentation.selectedItem);

    return mediaGridView;
  }

  private void fillRecommendationModel(Recommendation recommendation, AnnotatedImageCellFactory.Model model) {
    boolean hasContext = recommendation.work().getType().isComponent();

    fillRecommendationModel(
      recommendation,
      hasContext ? recommendation.work().getContext().map(Context::title).orElse(null) : null,
      recommendation.work().getDetails().getTitle(),
      !hasContext ? recommendation.work().getDetails().getReleaseDate().map(LocalDate::getYear).map(Object::toString).orElse(null) : null,
      model
    );
  }
}