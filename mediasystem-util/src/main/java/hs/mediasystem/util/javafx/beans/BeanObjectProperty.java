package hs.mediasystem.util.javafx.beans;

import javafx.beans.property.SimpleObjectProperty;

public final class BeanObjectProperty<T> extends SimpleObjectProperty<T> {
  private final BeanAccessor<T> accessor;

  public BeanObjectProperty(Object bean, String propertyName) {
    accessor = new BeanAccessor<>(bean, propertyName);
  }

  public void update() {
    synchronized(accessor) {
      super.set(accessor.read());
    }
  }

  @Override
  public T get() {
    synchronized(accessor) {
      T currentBeanValue = accessor.read();
      T currentValue = super.get();

      if(!currentBeanValue.equals(currentValue)) {
        super.set(currentBeanValue);
      }

      return super.get();
    }
  }

  @Override
  public void set(T value) {
    synchronized(accessor) {
      accessor.write(value);
      super.set(value);
    }
  }
}