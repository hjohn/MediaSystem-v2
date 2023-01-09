package hs.mediasystem.plugin.home;

import hs.mediasystem.ui.api.RecommendationClient;
import hs.mediasystem.ui.api.domain.Recommendation;

import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javax.inject.Inject;
import javax.inject.Singleton;

public class RecommendationsPresentation {
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
    this.selectedItem.set(recommendations.isEmpty() ? null : recommendations.get(0));
  }

  public List<Recommendation> getRecommendations() {
    return recommendations;
  }
}
