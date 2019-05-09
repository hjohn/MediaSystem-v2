package hs.mediasystem.framework.expose;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.property.Property;
import javafx.concurrent.Task;
import javafx.event.Event;

public class Expose {
  public static <P> ExposedMethod<P, Object>.ActionParentBuilder action(Consumer<P> consumer) {
    return action((P p, Event e) -> {
      consumer.accept(p);
      return null;
    });
  }

  public static <P> ExposedMethod<P, Object>.ActionParentBuilder action(BiConsumer<P, Event> consumer) {
    return action((P p, Event e) -> {
      consumer.accept(p, e);
      return null;
    });
  }

  public static <P, V> ExposedMethod<P, V>.ActionParentBuilder action(BiFunction<P, Event, Task<V>> consumer) {
    return new ExposedMethod<>(consumer).new ActionParentBuilder();
  }

  public static <P, T> ExposedProperty<P, T>.ObjectParentBuilder objectProperty(Function<P, Property<T>> function) {
    return new ExposedProperty<>(function).new ObjectParentBuilder();
  }

  public static <P> ExposedProperty<P, Boolean>.BooleanParentBuilder booleanProperty(Function<P, Property<Boolean>> function) {
    return new ExposedProperty<>(function).new BooleanParentBuilder();
  }

  public static <P> ExposedProperty<P, Long>.LongParentBuilder longProperty(Function<P, Property<Long>> function) {
    return new ExposedProperty<>(function).new LongParentBuilder();
  }

  public static <P> ExposedProperty<P, Double>.DoubleParentBuilder doubleProperty(Function<P, Property<Double>> function) {
    return new ExposedProperty<>(function).new DoubleParentBuilder();
  }

  public static <P> ExposedProperty<P, Number>.NumberParentBuilder numberProperty(Function<P, Property<Number>> function) {
    return new ExposedProperty<>(function).new NumberParentBuilder();
  }

  public static <P, T> ExposedProperty<P, T>.ListParentBuilder listProperty(Function<P, Property<T>> function) {
    return new ExposedProperty<>(function).new ListParentBuilder();
  }
}
