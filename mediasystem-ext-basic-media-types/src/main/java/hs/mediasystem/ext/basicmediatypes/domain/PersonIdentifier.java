package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identifier;

public class PersonIdentifier extends Identifier {

  public PersonIdentifier(DataSource dataSource, String id) {
    super(dataSource, id);
  }

}
