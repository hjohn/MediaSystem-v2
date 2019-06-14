package hs.mediasystem.framework.expose;

import hs.mediasystem.framework.actions.Formatter;

import java.util.ArrayList;
import java.util.function.Function;

import javafx.beans.property.Property;

public abstract class AbstractExposedProperty<P, T> extends AbstractExposedControl<P> {
  protected Formatter<T> formatter;

  private final Function<P, Property<T>> function;

  AbstractExposedProperty(Function<P, Property<T>> function) {
    this.function = function;
  }

  public Property<T> getProperty(P parent) {
    return function.apply(parent);
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
