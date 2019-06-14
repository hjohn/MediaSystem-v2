package hs.mediasystem.framework.expose;

import hs.mediasystem.framework.actions.Formatter;

import java.util.ArrayList;
import java.util.function.Function;

import javafx.beans.property.Property;
import javafx.collections.ObservableList;

public class ExposedListProperty<P, T> extends AbstractExposedProperty<P, T> {
  private Function<P, ObservableList<T>>allowedValues;

  ExposedListProperty(Function<P, Property<T>> function) {
    super(function);
  }

  public ObservableList<T> getAllowedValues(P parent) {
    return allowedValues.apply(parent);
  }

  public class ParentBuilder {
    public ListBuilder of(Class<? super P> cls) {
      ExposedListProperty.this.cls = cls;

      return new ListBuilder();
    }
  }

  public class ListBuilder {
    public ListBuilder allowedValues(Function<P, ObservableList<T>> allowedValues) {
      ExposedListProperty.this.allowedValues = allowedValues;

      return this;
    }

    public ListBuilder allowedValues(ObservableList<T> allowedValues) {
      ExposedListProperty.this.allowedValues = p -> allowedValues;

      return this;
    }

    public ListBuilder format(Formatter<T> formatter) {
      ExposedListProperty.this.formatter = formatter;

      return this;
    }

    public void as(String name) {
      ExposedListProperty.this.name = name;

      EXPOSED_PROPERTIES.computeIfAbsent(cls, k -> new ArrayList<>()).add(ExposedListProperty.this);
    }
  }

}
