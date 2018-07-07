package hs.mediasystem.util.javafx.beans;

import javafx.beans.property.SimpleDoubleProperty;

public final class BeanDoubleProperty extends SimpleDoubleProperty {
  private final Accessor<Double> accessor;

  private boolean initialized;

  public BeanDoubleProperty(Object bean, String propertyName) {
    accessor = new BeanAccessor<>(bean, propertyName);
  }

  public BeanDoubleProperty(Accessor<Double> accessor) {
    this.accessor = accessor;
  }

  @Override
  public double get() {
    if(!initialized) {
      initialized = true;
      super.set(accessor.read());
    }

    return super.get();
  }

  @Override
  public void set(double value) {
    super.set(value);
    accessor.write(value);
  }
}