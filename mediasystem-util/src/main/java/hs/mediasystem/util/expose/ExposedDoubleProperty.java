package hs.mediasystem.util.expose;

import java.util.ArrayList;
import java.util.function.Function;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ExposedDoubleProperty extends AbstractExposedNumericProperty<Double> {

  ExposedDoubleProperty(Function<Object, DoubleProperty> function) {
    super(cast(function));
  }

  @Override
  protected Double add(Double v1, Double v2) {
    return v1 + v2;
  }

  public class ParentBuilder<O> {
    public RangeBuilder of(Class<O> cls) {
      ExposedDoubleProperty.this.cls = cls;

      return new RangeBuilder();
    }
  }

  public class RangeBuilder {
    public RangeBuilder range(double min, double max, double step) {
      ExposedDoubleProperty.this.min = p -> new SimpleObjectProperty<>(min);
      ExposedDoubleProperty.this.max = p -> new SimpleObjectProperty<>(max);
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
