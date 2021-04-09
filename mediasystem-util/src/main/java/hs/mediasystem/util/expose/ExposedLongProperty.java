package hs.mediasystem.util.expose;

import java.util.ArrayList;
import java.util.function.Function;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableLongValue;
import javafx.beans.value.ObservableValue;

public class ExposedLongProperty<P> extends AbstractExposedNumericProperty<P, Long> {

  ExposedLongProperty(Function<P, LongProperty> function) {
    super(cast(function));
  }

  public class ParentBuilder {
    public RangeBuilder of(Class<P> cls) {
      ExposedLongProperty.this.cls = cls;

      return new RangeBuilder();
    }
  }

  public class RangeBuilder {
    public RangeBuilder range(long min, long max, long step) {
      ExposedLongProperty.this.min = p -> new SimpleObjectProperty<>(min);
      ExposedLongProperty.this.max = p -> new SimpleObjectProperty<>(max);
      ExposedLongProperty.this.step = Long.valueOf(step);

      return this;
    }

    @SuppressWarnings("unchecked")
    public RangeBuilder range(Function<P, ObservableLongValue> min, Function<P, ObservableLongValue> max, long step) {
      ExposedLongProperty.this.min = (Function<P, ObservableValue<Long>>)(Function<?, ?>)min;
      ExposedLongProperty.this.max = (Function<P, ObservableValue<Long>>)(Function<?, ?>)max;
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
