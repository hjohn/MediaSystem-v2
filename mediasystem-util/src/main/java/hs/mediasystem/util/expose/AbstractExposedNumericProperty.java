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

  public void add(Object ownerInstance, T value) {
    Property<T> property = getProperty(ownerInstance);

    property.setValue(clamp(ownerInstance, add(property.getValue(), value)));
  }

  protected abstract T add(T v1, T v2);

  private T clamp(Object ownerInstance, T value) {
    T minValue = getMin(ownerInstance).getValue();
    T maxValue = getMax(ownerInstance).getValue();

    if(value instanceof Long l) {  // special case for long to remain accurate
      if(l < minValue.longValue()) {
        return minValue;
      }
      if(l > maxValue.longValue()) {
        return maxValue;
      }

      return value;
    }

    if(value.doubleValue() < minValue.doubleValue()) {
      return minValue;
    }
    if(value.doubleValue() > maxValue.doubleValue()) {
      return maxValue;
    }

    return value;
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
