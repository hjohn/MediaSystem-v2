package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.ext.basicmediatypes.domain.Production;

public abstract class AbstractProductionDescriptor extends AbstractMediaDescriptor {
  private final Production production;

  public AbstractProductionDescriptor(Production production) {
    this.production = production;
  }

  public Production getProduction() {
    return production;
  }
}
