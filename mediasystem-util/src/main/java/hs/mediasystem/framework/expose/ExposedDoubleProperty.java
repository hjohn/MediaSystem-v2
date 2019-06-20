package hs.mediasystem.framework.expose;

import java.util.ArrayList;
import java.util.function.Function;

import javafx.beans.property.Property;

import org.reactfx.value.Val;

public class ExposedDoubleProperty<P> extends AbstractExposedNumericProperty<P, Double> {

  ExposedDoubleProperty(Function<P, Property<Double>> function) {
    super(function);
  }

  public class ParentBuilder {
    public RangeBuilder of(Class<P> cls) {
      ExposedDoubleProperty.this.cls = cls;

      return new RangeBuilder();
    }
  }

  public class RangeBuilder {
    public RangeBuilder range(double min, double max, double step) {
      ExposedDoubleProperty.this.min = p -> Val.constant(Double.valueOf(min));
      ExposedDoubleProperty.this.max = p -> Val.constant(Double.valueOf(max));
      ExposedDoubleProperty.this.step = Double.valueOf(step);

      return this;
    }

    public RangeBuilder format(Formatter<Double> formatter) {
      ExposedDoubleProperty.this.formatter = formatter;

      return this;
    }

    public void as(String name) {
      ExposedDoubleProperty.this.name = name;

      EXPOSED_PROPERTIES.computeIfAbsent(cls, k -> new ArrayList<>()).add(ExposedDoubleProperty.this);
    }
  }
}
