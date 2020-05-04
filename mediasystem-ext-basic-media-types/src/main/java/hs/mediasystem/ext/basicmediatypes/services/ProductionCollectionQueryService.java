package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;

import java.io.IOException;

public interface ProductionCollectionQueryService {
  ProductionCollection query(Identifier identifier) throws IOException;
}
