package hs.mediasystem.framework.expose;

import hs.mediasystem.framework.actions.Formatter;

import java.util.ArrayList;
import java.util.function.Function;

import javafx.beans.property.Property;

import org.reactfx.value.Val;

public class ExposedLongProperty<P> extends AbstractExposedNumericProperty<P, Long> {

  ExposedLongProperty(Function<P, Property<Long>> function) {
    super(function);
  }

  public class ParentBuilder {
    public RangeBuilder of(Class<P> cls) {
      ExposedLongProperty.this.cls = cls;

      return new RangeBuilder();
    }
  }

  public class RangeBuilder {
    public RangeBuilder range(long min, long max, long step) {
      ExposedLongProperty.this.min = p -> Val.constant(Long.valueOf(min));
      ExposedLongProperty.this.max = p -> Val.constant(Long.valueOf(max));
      ExposedLongProperty.this.step = Long.valueOf(step);

      return this;
    }

    public RangeBuilder format(Formatter<Long> formatter) {
      ExposedLongProperty.this.formatter = formatter;

      return this;
    }

    public void as(String name) {
      ExposedLongProperty.this.name = name;

      EXPOSED_PROPERTIES.computeIfAbsent(cls, k -> new ArrayList<>()).add(ExposedLongProperty.this);
    }
  }
}
