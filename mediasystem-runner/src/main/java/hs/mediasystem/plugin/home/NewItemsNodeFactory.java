package hs.mediasystem.plugin.home;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.plugin.cell.AnnotatedImageCellFactory;
import hs.mediasystem.presentation.PresentationLoader;
import hs.mediasystem.ui.api.RecommendationClient;
import hs.mediasystem.ui.api.domain.Recommendation;
import hs.mediasystem.util.javafx.control.ActionListView;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NewItemsNodeFactory extends AbstractCarouselNodeFactory {
  @Inject private RecommendationClient recommendationClient;

  public ActionListView<Recommendation> create() {
    HorizontalCarousel<Recommendation> mediaGridView = new HorizontalCarousel<>(
      recommendationClient.findNew(mediaType -> !mediaType.isComponent() && mediaType != MediaType.FOLDER && mediaType != MediaType.FILE),
      e -> PresentationLoader.navigate(e, () -> getRecommendedProductionPresentation(e.getItem())),
      new AnnotatedImageCellFactory<Recommendation>(this::fillRecommendationModel)
    );

    return mediaGridView;
  }

  private void fillRecommendationModel(Recommendation recommendation, AnnotatedImageCellFactory.Model model) {
    boolean hasParent = recommendation.getWork().getType().isComponent();

    fillRecommendationModel(
      recommendation,
      hasParent ? recommendation.getWork().getParent().map(p -> p.getName()).orElse(null) : null,
      recommendation.getWork().getDetails().getTitle(),
      !hasParent ? recommendation.getWork().getDetails().getReleaseDate().map(LocalDate::getYear).map(Object::toString).orElse(null) : null,
      model
    );
  }
}