package hs.mediasystem.util.javafx.beans;

import javafx.beans.property.SimpleFloatProperty;

public final class BeanFloatProperty extends SimpleFloatProperty {
  private final Accessor<Float> accessor;

  private boolean initialized;

  public BeanFloatProperty(Object bean, String propertyName) {
    accessor = new BeanAccessor<>(bean, propertyName);
  }

  public BeanFloatProperty(Accessor<Float> accessor) {
    this.accessor = accessor;
  }

  public void update() {
    synchronized(accessor) {
      super.set(accessor.read());
    }
  }

  @Override
  public float get() {
    synchronized(accessor) {
      if(!initialized) {
        initialized = true;
        super.set(accessor.read());
      }

      return super.get();
    }
  }

  @Override
  public void set(float value) {
    synchronized(accessor) {
      super.set(value);
      accessor.write(value);
    }
  }
}