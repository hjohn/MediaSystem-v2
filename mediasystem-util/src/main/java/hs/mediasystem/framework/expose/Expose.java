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

  public static <P, T> ExposedNode<P, T>.ParentBuilder nodeProperty(Function<P, Property<T>> function) {
    return new ExposedNode<>(function).new ParentBuilder();
  }

  public static <P> ExposedBooleanProperty<P>.ParentBuilder booleanProperty(Function<P, Property<Boolean>> function) {
    return new ExposedBooleanProperty<>(function).new ParentBuilder();
  }

  public static <P> ExposedLongProperty<P>.ParentBuilder longProperty(Function<P, Property<Long>> function) {
    return new ExposedLongProperty<>(function).new ParentBuilder();
  }

  public static <P> ExposedDoubleProperty<P>.ParentBuilder doubleProperty(Function<P, Property<Double>> function) {
    return new ExposedDoubleProperty<>(function).new ParentBuilder();
  }

  public static <P> ExposedNumberProperty<P>.ParentBuilder numberProperty(Function<P, Property<Number>> function) {
    return new ExposedNumberProperty<>(function).new ParentBuilder();
  }

  public static <P, T> ExposedListProperty<P, T>.ParentBuilder listProperty(Function<P, Property<T>> function) {
    return new ExposedListProperty<>(function).new ParentBuilder();
  }
}
