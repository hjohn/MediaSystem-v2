package hs.mediasystem.util.javafx.property;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;

public class SimpleReadOnlyObjectProperty<T> extends ReadOnlyObjectPropertyBase<T> {
  private final ObjectProperty<T> property;

  public SimpleReadOnlyObjectProperty(ObjectProperty<T> property) {
    this.property = property;
    this.property.addListener(obs -> fireValueChangedEvent());
  }

  @Override
  public Object getBean() {
    return property.getBean();
  }

  @Override
  public String getName() {
    return property.getName();
  }

  @Override
  public T get() {
    return property.get();
  }
}
