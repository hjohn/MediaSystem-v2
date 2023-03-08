package hs.mediasystem.presentation;

import hs.mediasystem.runner.util.debug.DebugFX;

public abstract class AbstractPresentation implements Presentation {

  {
    DebugFX.addReference(this);
  }
}
