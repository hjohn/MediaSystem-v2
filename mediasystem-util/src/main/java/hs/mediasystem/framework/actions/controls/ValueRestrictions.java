package hs.mediasystem.framework.actions.controls;

import org.reactfx.value.Val;

public class ValueRestrictions<T> {
  private final Val<T> min;
  private final Val<T> max;
  private final T step;

  public ValueRestrictions(Val<T> min, Val<T> max, T step) {
    this.min = min;
    this.max = max;
    this.step = step;
  }

  public ValueRestrictions(T min, T max, T step) {
    this(Val.constant(min), Val.constant(max), step);
  }

  public Val<T> getMinimum() {
    return min;
  }

  public Val<T> getMaximum() {
    return max;
  }

  public T getStepSize() {
    return step;
  }
}
