package hs.mediasystem.framework.expose;

import java.util.function.Function;

import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;

public abstract class AbstractExposedNumericProperty<P, T extends Number> extends AbstractExposedProperty<P, T> {
  protected Function<P, ObservableValue<T>> min;
  protected Function<P, ObservableValue<T>> max;
  protected T step;

  AbstractExposedNumericProperty(Function<P, Property<T>> function) {
    super(function);
  }

  public ObservableValue<T> getMin(P parent) {
    return min.apply(parent);
  }

  public ObservableValue<T> getMax(P parent) {
    return max.apply(parent);
  }

  public T getStep() {
    return step;
  }
}
