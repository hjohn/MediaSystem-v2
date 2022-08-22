package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.domain.Production;

import java.io.IOException;
import java.util.List;

public interface RecommendationQueryService {

  /**
   * Gets recommendations for a Movie or Serie.
   *
   * @param id a {@link WorkId}, cannot be null
   * @return a list of recommended Productions, never null
   * @throws IOException when an I/O problem occurred
   */
  List<Production> query(WorkId id) throws IOException;
}
