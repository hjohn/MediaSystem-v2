package hs.mediasystem.util.expose;

import java.util.ArrayList;
import java.util.function.Function;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableLongValue;
import javafx.beans.value.ObservableValue;

public class ExposedLongProperty extends AbstractExposedNumericProperty<Long> {

  ExposedLongProperty(Function<Object, LongProperty> function) {
    super(cast(function));
  }

  @Override
  public Long add(Long v1, Long v2) {
    return v1 + v2;
  }

  public class ParentBuilder<O> {
    public RangeBuilder<O> of(Class<O> cls) {
      ExposedLongProperty.this.cls = cls;

      return new RangeBuilder<>();
    }
  }

  public class RangeBuilder<O> {
    public RangeBuilder<O> range(long min, long max, long step) {
      ExposedLongProperty.this.min = p -> new SimpleObjectProperty<>(min);
      ExposedLongProperty.this.max = p -> new SimpleObjectProperty<>(max);
      ExposedLongProperty.this.step = Long.valueOf(step);

      return this;
    }

    @SuppressWarnings("unchecked")
    public RangeBuilder<O> range(Function<O, ObservableLongValue> min, Function<O, ObservableLongValue> max, long step) {
      ExposedLongProperty.this.min = (Function<Object, ObservableValue<Long>>)(Function<?, ?>)min;
      ExposedLongProperty.this.max = (Function<Object, ObservableValue<Long>>)(Function<?, ?>)max;
      ExposedLongProperty.this.step = Long.valueOf(step);

      return this;
    }

    public RangeBuilder<O> format(Formatter<Long> formatter) {
      ExposedLongProperty.this.formatter = formatter;

      return this;
    }

    public void as(String name) {
      ExposedLongProperty.this.name = name;

      EXPOSED_PROPERTIES.computeIfAbsent(cls, k -> new ArrayList<>()).add(ExposedLongProperty.this);
    }
  }
}
