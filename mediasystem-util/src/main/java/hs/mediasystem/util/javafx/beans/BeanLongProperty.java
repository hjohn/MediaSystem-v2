package hs.mediasystem.util.javafx.beans;

import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;

public final class BeanLongProperty extends SimpleLongProperty {
  private final Accessor<Long> accessor;

  private boolean initialized;

  public BeanLongProperty(Object bean, String propertyName) {
    accessor = new BeanAccessor<>(bean, propertyName);
  }

  public BeanLongProperty(Accessor<Long> accessor) {
    if(accessor == null) {
      throw new IllegalArgumentException("parameter 'accessor' cannot be null");
    }
    this.accessor = accessor;
  }

  @Override
  public long get() {
    if(!initialized) {
      initialized = true;
      Long value = accessor.read();
      super.set(value == null ? 0 : value);
    }

    return super.get();
  }

  @Override
  public void set(long value) {
    super.set(value);
    accessor.write(value);
  }

  public void update(final long value) {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        BeanLongProperty.super.set(value);
      }
    });
  }
}