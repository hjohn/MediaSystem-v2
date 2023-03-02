package hs.mediasystem.util.expose;

import java.util.ArrayList;
import java.util.function.Function;

import javafx.beans.property.Property;
import javafx.collections.ObservableList;

public class ExposedListProperty<T> extends AbstractExposedProperty<T> {
  private Function<Object, ObservableList<T>> allowedValues;

  ExposedListProperty(Function<Object, Property<T>> function) {
    super(function);
  }

  public ObservableList<T> getAllowedValues(Object ownerInstance) {
    return allowedValues.apply(ownerInstance);
  }

  public class ParentBuilder<O> {
    public ListBuilder<O> of(Class<? super O> cls) {
      ExposedListProperty.this.cls = cls;

      return new ListBuilder<>();
    }
  }

  public class ListBuilder<O> {
    @SuppressWarnings("unchecked")
    public ListBuilder<O> allowedValues(Function<O, ObservableList<T>> allowedValues) {
      ExposedListProperty.this.allowedValues = (Function<Object, ObservableList<T>>)allowedValues;

      return this;
    }

    public ListBuilder<O> allowedValues(ObservableList<T> allowedValues) {
      ExposedListProperty.this.allowedValues = p -> allowedValues;

      return this;
    }

    public ListBuilder<O> format(Formatter<T> formatter) {
      ExposedListProperty.this.formatter = formatter;

      return this;
    }

    public void as(String name) {
      ExposedListProperty.this.name = name;

      EXPOSED_PROPERTIES.computeIfAbsent(cls, k -> new ArrayList<>()).add(ExposedListProperty.this);
    }
  }

}
