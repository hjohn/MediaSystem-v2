package hs.mediasystem.plugin.home;

import hs.mediasystem.plugin.home.HomePresentation.OptionsPresentation;
import hs.mediasystem.ui.api.RecommendationClient;
import hs.mediasystem.ui.api.domain.Context;
import hs.mediasystem.ui.api.domain.Recommendation;
import hs.mediasystem.util.image.ImageHandle;

import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import javax.inject.Inject;
import javax.inject.Singleton;

public final class RecommendationsPresentation implements OptionsPresentation {
  public final ObjectProperty<Recommendation> selectedItem = new SimpleObjectProperty<>();

  private final List<Recommendation> recommendations;

  @Singleton
  public static class Factory {
    @Inject private RecommendationClient recommendationClient;

    public RecommendationsPresentation create() {
      return new RecommendationsPresentation(recommendationClient.findRecommendations(100));
    }
  }

  public RecommendationsPresentation(List<Recommendation> recommendations) {
    this.recommendations = recommendations;
    this.selectedItem.set(recommendations.isEmpty() ? null : recommendations.getFirst());
  }

  public List<Recommendation> getRecommendations() {
    return recommendations;
  }

  @Override
  public ObservableValue<ImageHandle> backdropProperty() {
    return selectedItem.map(r -> r.work().getContext()
      .filter(c -> c.type().isSerie())
      .flatMap(Context::backdrop)
      .or(() -> r.work().getDetails().getBackdrop())
      .orElse(null)
    );
  }
}
