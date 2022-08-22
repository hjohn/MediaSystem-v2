package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;

import java.io.IOException;

public interface ProductionCollectionQueryService {
  ProductionCollection query(WorkId id) throws IOException;
}
