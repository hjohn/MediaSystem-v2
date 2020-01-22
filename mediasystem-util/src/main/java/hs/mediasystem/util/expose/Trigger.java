package hs.mediasystem.util.expose;

import java.util.function.Consumer;
import java.util.function.Function;

import javafx.concurrent.Task;
import javafx.event.Event;

public interface Trigger<V> {

  /**
   * Runs the action associated with the trigger either directly or via the supplied taskRunner,
   * depending on whether the trigger action runs synchronous or asynchronous.
   *
   * @param taskRunner a function that can run a {@link Task}
   * @return the resulting value if any
   */
  V run(Event event, Function<Task<V>, V> taskRunner);

  public static <V> Trigger<V> synchronous(Function<Event, V> task) {
    return new Trigger<>() {
      @Override
      public V run(Event event, Function<Task<V>, V> taskRunner) {
        return task.apply(event);
      }
    };
  }

  public static <V> Trigger<V> asynchronous(Function<Event, V> task) {
    return new Trigger<>() {
      @Override
      public V run(Event event, Function<Task<V>, V> taskRunner) {
        return taskRunner.apply(new Task<>() {
          @Override
          protected V call() throws Exception {
            task.apply(event);

            return null;
          }
        });
      }
    };
  }

  public static <V> Trigger<V> asynchronous(Consumer<Event> task) {
    return asynchronous(e -> { task.accept(e); return null; });
  }
}
