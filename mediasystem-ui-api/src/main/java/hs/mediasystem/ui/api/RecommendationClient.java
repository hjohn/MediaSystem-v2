package hs.mediasystem.ui.api;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ui.api.domain.Recommendation;

import java.util.List;
import java.util.function.Predicate;

public interface RecommendationClient {
  List<Recommendation> findRecommendations(int maximum);
  List<Recommendation> findNew(Predicate<MediaType> filter);
}
