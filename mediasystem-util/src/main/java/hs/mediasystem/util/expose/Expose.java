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
   * Creates an indirect action. This allows the task caller to decide how the task
   * is run (in a nested event loop for example) and to control when the action is
   * available. Returning {@code null} from the supplier indicates the action is currently
   * unavailable.
   *
   * @param <O> the type of presentation this action applies to
   * @param <V> the type of the value the action returns
   * @param supplier the {@link Trigger} supplier, cannot be {@code null}
   * @return a fluent builder, never {@code null}
   */
  public static <O, V> ExposedMethod<V>.ActionParentBuilder<O> indirectAction(Function<O, Trigger<V>> supplier) {
    return new ExposedMethod<>(cast(supplier)).new ActionParentBuilder<>();
  }

  /**
   * Creates a direct action without result which consumes the event that triggered it automatically.
   *
   * @param <O> the type of presentation this action applies to
   * @param consumer the consumer to call, cannot be {@code null}
   * @return a fluent builder, never {@code null}
   */
  public static <O> ExposedMethod<Object>.ActionParentBuilder<O> action(Consumer<O> consumer) {
    return indirectAction(ownerInstance -> Trigger.synchronous(e -> {
      consumer.accept(ownerInstance);
      e.consume();

      return null;
    }));
  }

  /**
   * Creates a direct action without result which is passed the event that triggered it. The action
   * should consume the event as needed when it is called.
   *
   * @param <O> the type of presentation this action applies to
   * @param consumer the consumer to call, cannot be {@code null}
   * @return a fluent builder, never {@code null}
   */
  public static <O> ExposedMethod<Object>.ActionParentBuilder<O> action(BiConsumer<O, Event> consumer) {
    return indirectAction(ownerInstance -> Trigger.synchronous(e -> {
      consumer.accept(ownerInstance, e);

      return null;
    }));
  }

  public static <O, T> ExposedNode<T>.ParentBuilder<O> nodeProperty(Function<O, Property<T>> function) {
    return new ExposedNode<>(cast(function)).new ParentBuilder<>();
  }

  public static <O> ExposedBooleanProperty.ParentBuilder<O> booleanProperty(Function<O, Property<Boolean>> function) {
    return new ExposedBooleanProperty(cast(function)).new ParentBuilder<>();
  }

  public static <O> ExposedLongProperty.ParentBuilder<O> longProperty(Function<O, LongProperty> function) {
    return new ExposedLongProperty(cast(function)).new ParentBuilder<>();
  }

  public static <O> ExposedDoubleProperty.ParentBuilder<O> doubleProperty(Function<O, DoubleProperty> function) {
    return new ExposedDoubleProperty(cast(function)).new ParentBuilder<>();
  }

  public static <O> ExposedNumberProperty.ParentBuilder<O> numberProperty(Function<O, Property<Number>> function) {
    return new ExposedNumberProperty(cast(function)).new ParentBuilder<>();
  }

  public static <O, T> ExposedListProperty<T>.ParentBuilder<O> listProperty(Function<O, Property<T>> function) {
    return new ExposedListProperty<>(cast(function)).new ParentBuilder<>();
  }

  // Removes the owner type parameter
  @SuppressWarnings("unchecked")
  private static <X> Function<Object, X> cast(Function<?, X> function) {
    return (Function<Object, X>)function;
  }
}
