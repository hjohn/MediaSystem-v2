package hs.mediasystem.util.javafx.beans;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;

public final class BeanBooleanProperty extends SimpleBooleanProperty {
  private final Accessor<Boolean> accessor;

  private boolean initialized;

  public BeanBooleanProperty(Object bean, String propertyName) {
    accessor = new BeanAccessor<>(bean, propertyName);
  }

  public BeanBooleanProperty(Accessor<Boolean> accessor) {
    this.accessor = accessor;
  }

  @Override
  public boolean get() {
    if(!initialized) {
      initialized = true;
      super.set(accessor.read());
    }

    return super.get();
  }

  @Override
  public void set(boolean value) {
    super.set(value);
    accessor.write(value);
  }

  public void update(final boolean value) {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        BeanBooleanProperty.super.set(value);
      }
    });
  }
}