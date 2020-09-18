package hs.mediasystem.plugin.home;

import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.plugin.cell.AnnotatedImageCellFactory;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory.ProductionPresentation;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory.State;
import hs.mediasystem.ui.api.domain.Recommendation;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.SizeFormatter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import javax.inject.Inject;

public abstract class AbstractCarouselNodeFactory {
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private ProductionPresentationFactory productionPresentationFactory;

  protected void fillRecommendationModel(Recommendation recommendation, String parentTitle, String title, String subtitle, AnnotatedImageCellFactory.Model model) {
    model.parentTitle.set(parentTitle);
    model.title.set(title);
    model.subtitle.set(subtitle);
    model.sequence.set(recommendation.getWork().getDetails().getSequence()
      .map(seq -> seq.getSeasonNumber().map(s -> s + "x").orElse("") + seq.getNumber())
      .orElse(null)
    );
    model.imageHandle.set(recommendation.getWork().getDetails().getBackdrop().map(imageHandleFactory::fromURI).orElse(null));

    double fraction = recommendation.getLength().map(len -> recommendation.getPosition().toSeconds() / (double)len.toSeconds()).orElse(0.0);

    model.watchedFraction.set(recommendation.isWatched() ? 1.0 : fraction > 0 ? fraction : -1);
    model.age.set(Optional.of(recommendation.getSampleTime())
      .map(i -> i.atZone(ZoneId.systemDefault()))
      .map(ZonedDateTime::toLocalDateTime)
      .map(SizeFormatter::formatTimeAgo)
      .orElse(null)
    );
  }

  protected ProductionPresentation getRecommendedProductionPresentation(Recommendation r) {
    boolean hasParent = r.getWork().getType().isComponent();
    WorkId id = hasParent ?
        r.getWork().getParent().map(Parent::getId).orElseThrow() :
        r.getWork().getId();

    return productionPresentationFactory.create(id, hasParent ? State.EPISODE : State.OVERVIEW, hasParent ? r.getWork().getId() : null);
  }
}
