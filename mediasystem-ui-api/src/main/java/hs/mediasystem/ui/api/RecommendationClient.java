package hs.mediasystem.ui.api;

import hs.mediasystem.ui.api.domain.Recommendation;

import java.util.List;

public interface RecommendationClient {
  List<Recommendation> findRecommendations(int maximum);
  List<Recommendation> findNew();
}
