package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identifier;

public class ProductionIdentifier extends Identifier {

  public ProductionIdentifier(DataSource dataSource, String id) {
    super(dataSource, id);
  }

}
