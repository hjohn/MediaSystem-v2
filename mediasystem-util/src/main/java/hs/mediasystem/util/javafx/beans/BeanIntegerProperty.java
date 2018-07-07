package hs.mediasystem.util.javafx.beans;

import javafx.beans.property.SimpleIntegerProperty;

public final class BeanIntegerProperty extends SimpleIntegerProperty {
  private final Accessor<Integer> accessor;

  private boolean initialized;

  public BeanIntegerProperty(Object bean, String propertyName) {
    accessor = new BeanAccessor<>(bean, propertyName);
  }

  public BeanIntegerProperty(Accessor<Integer> accessor) {
    this.accessor = accessor;
  }

  public void update() {
    synchronized(accessor) {
      super.set(accessor.read());
    }
  }

  @Override
  public int get() {
    synchronized(accessor) {
      if(!initialized) {
        initialized = true;
        super.set(accessor.read());
      }

      return super.get();
    }
  }

  @Override
  public void set(int value) {
    synchronized(accessor) {
      super.set(value);
      accessor.write(value);
    }
  }
}