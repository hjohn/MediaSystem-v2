package hs.mediasystem.util.expose;

import java.util.function.Function;

import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;

public abstract class AbstractExposedNumericProperty<T extends Number> extends AbstractExposedProperty<T> {
  protected Function<Object, ObservableValue<T>> min;
  protected Function<Object, ObservableValue<T>> max;
  protected T step;

  AbstractExposedNumericProperty(Function<Object, Property<T>> function) {
    super(function);
  }

  @SuppressWarnings("unchecked")
  protected static <T> Function<Object, Property<T>> cast(Function<Object, ? extends Property<?>> function) {
    return (Function<Object, Property<T>>)function;
  }

  public ObservableValue<T> getMin(Object ownerInstance) {
    return min.apply(ownerInstance);
  }

  public ObservableValue<T> getMax(Object ownerInstance) {
    return max.apply(ownerInstance);
  }

  public T getStep() {
    return step;
  }
}
