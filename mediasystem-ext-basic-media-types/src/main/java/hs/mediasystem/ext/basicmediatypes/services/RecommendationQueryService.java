package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;

import java.io.IOException;
import java.util.List;

public interface RecommendationQueryService {

  /**
   * Gets recommendations for a Movie or Serie.
   *
   * @param identifier a Movie or a Serie identifier, cannot be null
   * @return a list of recommended Productions, never null
   * @throws IOException when an I/O problem occurred
   */
  List<Production> query(ProductionIdentifier identifier) throws IOException;
}
