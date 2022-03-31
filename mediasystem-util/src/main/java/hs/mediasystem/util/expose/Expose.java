package hs.mediasystem.util.expose;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.Property;
import javafx.event.Event;

public class Expose {

  /**
   * Creates an indirect action. This allows running the action on a background thread and to control
   * when the action is available. Returning {@code null} from the supplier indicates the action is currently
   * unavailable.
   *
   * @param <P> the type of presentation this action applies to
   * @param <V> the type of the value the action returns
   * @param supplier the {@link Trigger} supplier, cannot be {@code null}
   * @return a fluent builder, never {@code null}
   */
  public static <P, V> ExposedMethod<P, V>.ActionParentBuilder indirectAction(Function<P, Trigger<V>> supplier) {
    return new ExposedMethod<>(supplier).new ActionParentBuilder();
  }

  public static <P> ExposedMethod<P, Object>.ActionParentBuilder action(Consumer<P> consumer) {
    return indirectAction((P p) -> Trigger.synchronous(e -> { consumer.accept(p); return null; }));
  }

  public static <P> ExposedMethod<P, Object>.ActionParentBuilder action(BiConsumer<P, Event> consumer) {
    return indirectAction((P p) -> Trigger.synchronous(e -> { consumer.accept(p, e); return null; }));
  }

  public static <P, T> ExposedNode<P, T>.ParentBuilder nodeProperty(Function<P, Property<T>> function) {
    return new ExposedNode<>(function).new ParentBuilder();
  }

  public static <P> ExposedBooleanProperty<P>.ParentBuilder booleanProperty(Function<P, Property<Boolean>> function) {
    return new ExposedBooleanProperty<>(function).new ParentBuilder();
  }

  public static <P> ExposedLongProperty<P>.ParentBuilder longProperty(Function<P, LongProperty> function) {
    return new ExposedLongProperty<>(function).new ParentBuilder();
  }

  public static <P> ExposedDoubleProperty<P>.ParentBuilder doubleProperty(Function<P, DoubleProperty> function) {
    return new ExposedDoubleProperty<>(function).new ParentBuilder();
  }

  public static <P> ExposedNumberProperty<P>.ParentBuilder numberProperty(Function<P, Property<Number>> function) {
    return new ExposedNumberProperty<>(function).new ParentBuilder();
  }

  public static <P, T> ExposedListProperty<P, T>.ParentBuilder listProperty(Function<P, Property<T>> function) {
    return new ExposedListProperty<>(function).new ParentBuilder();
  }
}
