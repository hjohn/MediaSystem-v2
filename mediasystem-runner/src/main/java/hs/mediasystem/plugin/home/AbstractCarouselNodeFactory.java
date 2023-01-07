package hs.mediasystem.plugin.home;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.plugin.cell.AnnotatedImageCellFactory;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory.ProductionPresentation;
import hs.mediasystem.ui.api.domain.Recommendation;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.natural.SizeFormatter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import javax.inject.Inject;

public abstract class AbstractCarouselNodeFactory {
  @Inject private ProductionPresentationFactory productionPresentationFactory;

  protected void fillRecommendationModel(Recommendation recommendation, String parentTitle, String title, String subtitle, AnnotatedImageCellFactory.Model model) {
    Work work = recommendation.work();

    model.parentTitle.set(parentTitle);
    model.title.set(title);
    model.subtitle.set(subtitle);
    model.sequence.set(work.getDetails().getSequence()
      .map(seq -> seq.seasonNumber().map(s -> s + "x").orElse("") + seq.number())
      .orElse(null)
    );

    // This has an exception for Movies, as the sample images for movies are often very wide aspect which does
    // not look that nice.  Also, recognizable images are preferred.  Perhaps introduce an alternative backdrop
    // image so the main backdrop and the image in the preview does not necessarily need to be the same.
    model.imageHandle.set((work.getType() == MediaType.MOVIE ? work.getDetails().getBackdrop() : work.getDetails().getSampleImage()).or(() -> work.getDetails().getBackdrop()).orElse(null));

    double fraction = work.getWatchedFraction().orElse(-1);

    model.watchedFraction.set(work.isWatched() ? 1.0 : fraction > 0 ? fraction : -1);
    model.age.set(Optional.of(recommendation.sampleTime())
      .map(i -> i.atZone(ZoneId.systemDefault()))
      .map(ZonedDateTime::toLocalDateTime)
      .map(SizeFormatter::formatTimeAgo)
      .orElse(null)
    );
  }

  protected ProductionPresentation getRecommendedProductionPresentation(Recommendation r) {
    return productionPresentationFactory.create(r.work().getId());
  }
}
