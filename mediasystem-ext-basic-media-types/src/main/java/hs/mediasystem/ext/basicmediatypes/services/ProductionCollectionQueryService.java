package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;

public interface ProductionCollectionQueryService {
  ProductionCollection query(Identifier identifier);
}
