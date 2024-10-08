package hs.mediasystem.util.expose;

import java.util.function.Function;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;

public class ExposedNumberProperty extends AbstractExposedNumericProperty<Number> {

  ExposedNumberProperty(Function<Object, Property<Number>> function) {
    super(function);
  }

  @Override
  protected Number add(Number v1, Number v2) {
    if(v1 instanceof Long l1 && v2 instanceof Long l2) {
      return l1 + l2;
    }

    return v1.doubleValue() + v2.doubleValue();
  }

  public class ParentBuilder<O> {
    public RangeBuilder of(Class<O> cls) {
      ExposedNumberProperty.this.cls = cls;

      return new RangeBuilder();
    }
  }

  public class RangeBuilder {
    public NameBuilder range(double min, double max, double step) {
      SimpleDoubleProperty minProperty = new SimpleDoubleProperty(min);
      SimpleDoubleProperty maxProperty = new SimpleDoubleProperty(max);

      ExposedNumberProperty.this.min = p -> minProperty;
      ExposedNumberProperty.this.max = p -> maxProperty;
      ExposedNumberProperty.this.step = Double.valueOf(step);

      return new NameBuilder();
    }
  }
}
