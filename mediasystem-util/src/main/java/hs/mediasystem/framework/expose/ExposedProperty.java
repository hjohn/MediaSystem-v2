package hs.mediasystem.framework.expose;

import hs.mediasystem.framework.actions.Formatter;

import java.util.ArrayList;
import java.util.function.Function;

import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

import org.reactfx.value.Val;

public class ExposedProperty<P, T> extends AbstractExposedControl<P> {
  private Function<P, Property<T>> function;
  private Function<P, ObservableValue<T>> min;
  private Function<P, ObservableValue<T>> max;
  private T step;
  private Function<P, ObservableList<T>>allowedValues;
  private Class<T> providedType;
  private boolean isProvidingSubType;
  private boolean isTriState;
  private boolean allowSelectTriState;
  private Formatter<T> formatter;

  ExposedProperty(Function<P, Property<T>> function) {
    this.function = function;
  }

  public Property<T> getProperty(P parent) {
    return function.apply(parent);
  }

  @Override
  public Class<T> getProvidedType() {
    return providedType;
  }

  public boolean isProvidingSubType() {
    return isProvidingSubType;
  }

  public boolean isTriState() {
    return isTriState;
  }

  public boolean allowSelectTriState() {
    return allowSelectTriState;
  }

  public ObservableValue<T> getMin(P parent) {
    return min.apply(parent);
  }

  public ObservableValue<T> getMax(P parent) {
    return max.apply(parent);
  }

  public T getStep() {
    return step;
  }

  public ObservableList<T> getAllowedValues(P parent) {
    return allowedValues.apply(parent);
  }

  public Formatter<T> getFormatter() {
    return formatter;
  }

  public class ObjectParentBuilder {
    public ProvidesBuilder of(Class<P> cls) {
      ExposedProperty.this.cls = cls;

      return new ProvidesBuilder();
    }
  }

  public class BooleanParentBuilder {
    @SuppressWarnings("unchecked")
    public BooleanRangeBuilder of(Class<? super P> cls) {
      ExposedProperty.this.cls = cls;
      ExposedProperty.this.providedType = (Class<T>)Boolean.class;
      ExposedProperty.this.type = Type.BOOLEAN;

      return new BooleanRangeBuilder();
    }
  }

  public class LongParentBuilder {
    @SuppressWarnings("unchecked")
    public LongRangeBuilder of(Class<P> cls) {
      ExposedProperty.this.cls = cls;
      ExposedProperty.this.providedType = (Class<T>)Long.class;
      ExposedProperty.this.type = Type.NUMERIC;

      return new LongRangeBuilder();
    }
  }

  public class DoubleParentBuilder {
    @SuppressWarnings("unchecked")
    public DoubleRangeBuilder of(Class<P> cls) {
      ExposedProperty.this.cls = cls;
      ExposedProperty.this.providedType = (Class<T>)Double.class;
      ExposedProperty.this.type = Type.NUMERIC;

      return new DoubleRangeBuilder();
    }
  }

  public class NumberParentBuilder {
    @SuppressWarnings("unchecked")
    public NumberRangeBuilder of(Class<P> cls) {
      ExposedProperty.this.cls = cls;
      ExposedProperty.this.providedType = (Class<T>)Number.class;
      ExposedProperty.this.type = Type.NUMERIC;

      return new NumberRangeBuilder();
    }
  }

  public class ListParentBuilder {
    public ListBuilder of(Class<? super P> cls) {
      ExposedProperty.this.cls = cls;
      ExposedProperty.this.type = Type.LIST;

      return new ListBuilder();
    }
  }

  public class LongRangeBuilder {
    @SuppressWarnings("unchecked")
    public LongRangeBuilder range(long min, long max, long step) {
      ExposedProperty.this.min = p -> Val.constant((T)Long.valueOf(min));
      ExposedProperty.this.max = p -> Val.constant((T)Long.valueOf(max));
      ExposedProperty.this.step = (T)Long.valueOf(step);

      return this;
    }

    public LongRangeBuilder format(Formatter<T> formatter) {
      ExposedProperty.this.formatter = formatter;

      return this;
    }

    public void as(String name) {
      ExposedProperty.this.name = name;

      EXPOSED_PROPERTIES.computeIfAbsent(cls, k -> new ArrayList<>()).add(ExposedProperty.this);
    }
  }

  public class DoubleRangeBuilder {
    @SuppressWarnings("unchecked")
    public DoubleRangeBuilder range(double min, double max, double step) {
      ExposedProperty.this.min = p -> Val.constant((T)Double.valueOf(min));
      ExposedProperty.this.max = p -> Val.constant((T)Double.valueOf(max));
      ExposedProperty.this.step = (T)Double.valueOf(step);

      return this;
    }

    public DoubleRangeBuilder format(Formatter<T> formatter) {
      ExposedProperty.this.formatter = formatter;

      return this;
    }

    public void as(String name) {
      ExposedProperty.this.name = name;

      EXPOSED_PROPERTIES.computeIfAbsent(cls, k -> new ArrayList<>()).add(ExposedProperty.this);
    }
  }

  public class NumberRangeBuilder {
    @SuppressWarnings("unchecked")
    public NameBuilder range(double min, double max, double step) {
      ExposedProperty.this.min = p -> Val.constant((T)Double.valueOf(min));
      ExposedProperty.this.max = p -> Val.constant((T)Double.valueOf(max));
      ExposedProperty.this.step = (T)Double.valueOf(step);

      return new NameBuilder();
    }
  }

  public class BooleanRangeBuilder {
    public BooleanRangeBuilder asTriState(boolean allowSelectTriState) {
      ExposedProperty.this.isTriState = true;
      ExposedProperty.this.allowSelectTriState = allowSelectTriState;

      return this;
    }

    public void as(String name) {
      ExposedProperty.this.name = name;

      EXPOSED_PROPERTIES.computeIfAbsent(cls, k -> new ArrayList<>()).add(ExposedProperty.this);
    }
  }

  public class ListBuilder {
    public ListBuilder allowedValues(Function<P, ObservableList<T>> allowedValues) {
      ExposedProperty.this.allowedValues = allowedValues;

      return this;
    }

    public ListBuilder allowedValues(ObservableList<T> allowedValues) {
      ExposedProperty.this.allowedValues = p -> allowedValues;

      return this;
    }

    public ListBuilder format(Formatter<T> formatter) {
      ExposedProperty.this.formatter = formatter;

      return this;
    }

    public void as(String name) {
      ExposedProperty.this.name = name;

      EXPOSED_PROPERTIES.computeIfAbsent(cls, k -> new ArrayList<>()).add(ExposedProperty.this);
    }
  }

  public class ProvidesBuilder {
    public NameBuilder provides(Class<T> cls) {
      ExposedProperty.this.providedType = cls;
      ExposedProperty.this.isProvidingSubType = true;

      return new NameBuilder();
    }
  }

  public class NameBuilder {
    public void as(String name) {
      ExposedProperty.this.name = name;

      EXPOSED_PROPERTIES.computeIfAbsent(cls, k -> new ArrayList<>()).add(ExposedProperty.this);
    }
  }
}
