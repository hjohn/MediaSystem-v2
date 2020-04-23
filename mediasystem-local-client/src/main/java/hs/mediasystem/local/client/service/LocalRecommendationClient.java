package hs.mediasystem.local.client.service;

import hs.mediasystem.db.services.RecommendationService;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ui.api.RecommendationClient;
import hs.mediasystem.ui.api.domain.Recommendation;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalRecommendationClient implements RecommendationClient {
  @Inject private RecommendationService service;
  @Inject private LocalWorksClient worksClient;

  @Override
  public List<Recommendation> findRecommendations(int maximum) {
    return service.findRecommendations(maximum).stream().map(this::toRecommendation).collect(Collectors.toList());
  }

  @Override
  public List<Recommendation> findNew(Predicate<MediaType> filter) {
    return service.findNew(filter).stream().map(this::toRecommendation).collect(Collectors.toList());
  }

  private Recommendation toRecommendation(hs.mediasystem.ext.basicmediatypes.domain.stream.Recommendation r) {
    return new Recommendation(
      worksClient.toWork(r.getWork(), r.getParent().orElse(null)),
      r.getInstant(),
      r.isWatched(),
      r.getLength().orElse(null),
      r.getPosition()
    );
  }
}
