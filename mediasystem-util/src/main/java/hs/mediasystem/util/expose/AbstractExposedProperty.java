package hs.mediasystem.util.expose;

import java.util.ArrayList;
import java.util.function.Function;

import javafx.beans.property.Property;

public abstract class AbstractExposedProperty<T> extends AbstractExposedControl {
  protected Formatter<T> formatter;

  private final Function<Object, Property<T>> function;

  AbstractExposedProperty(Function<Object, Property<T>> function) {
    this.function = function;
  }

  public Property<T> getProperty(Object ownerInstance) {
    return function.apply(ownerInstance);
  }

  public Formatter<T> getFormatter() {
    return formatter;
  }

  public class NameBuilder {
    public void as(String name) {
      AbstractExposedProperty.this.name = name;

      EXPOSED_PROPERTIES.computeIfAbsent(cls, k -> new ArrayList<>()).add(AbstractExposedProperty.this);
    }
  }
}
