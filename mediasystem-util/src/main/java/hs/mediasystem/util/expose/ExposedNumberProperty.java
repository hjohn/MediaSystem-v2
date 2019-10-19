package hs.mediasystem.util.expose;

import java.util.function.Function;

import javafx.beans.property.Property;

import org.reactfx.value.Val;

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
      ExposedNumberProperty.this.min = p -> Val.constant(Double.valueOf(min));
      ExposedNumberProperty.this.max = p -> Val.constant(Double.valueOf(max));
      ExposedNumberProperty.this.step = Double.valueOf(step);

      return new NameBuilder();
    }
  }
}
