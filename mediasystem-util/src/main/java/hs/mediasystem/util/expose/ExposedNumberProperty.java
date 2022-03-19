package hs.mediasystem.util.expose;

import java.util.function.Function;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;

public class ExposedNumberProperty<P> extends AbstractExposedNumericProperty<P, Number> {

  ExposedNumberProperty(Function<P, Property<Number>> function) {
    super(function);
  }

  public class ParentBuilder {
    public RangeBuilder of(Class<P> cls) {
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
