package hs.mediasystem.framework.actions.controls;

import hs.mediasystem.framework.actions.Formatter;

import org.reactfx.value.Var;

public interface Control<T> extends Var<T> {

  // isSorted
  // isDiscrete
  // isContinuous

  Formatter<T> getFormatter();
}
