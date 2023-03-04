package hs.mediasystem.util.expose;

import java.util.function.Consumer;
import java.util.function.Function;

import javafx.concurrent.Task;
import javafx.event.Event;

/**
 * Represents a potential task to execute.<p>
 *
 * Synchronous tasks are intended to be executed on the caller thread. They should
 * take care to not block the caller too long, either by doing only a small amount
 * of work, doing their work on a separate thread, or by using a nested event loop.<p>
 *
 * Asynchronous tasks are run via a task runner supplied by the caller. This gives
 * the caller control over whether to run the task directly, on a background thread,
 * or as a nested event loop.<p>
 *
 * All tasks are provided an {@link Event} when they're called. Tasks should consume
 * this event if their execution completes. Tasks that are missing pre-requisites
 * can choose not to consume the event to potentially allow another task to be
 * triggered.<p>
 *
 * Note that if the task runner used doesn't block the caller thread, the task runner
 * should immediately consume the event and provide a different event to the task.
 * The task won't be able to give feedback about its consumption state to the caller
 * in this case.
 *
 * @param <V> the type of result the task returns
 */
public interface Trigger<V> {

  /**
   * Runs the task associated with the trigger, and returns a result on completion.<p>
   *
   * Asynchronous tasks are run via the supplied {@code taskRunner}, allowing the caller
   * to run the task on a separate thread, and/or as part of a nested event loop.
   * Depending on the given {@code taskRunner} this can mean this call blocks (when
   * run on a nested event loop) or returns immediately.<p>
   *
   * The returned result for synchronous tasks is the result the task returns. For
   * asynchronous tasks, this is up to the {@code taskRunner} used. If the runner
   * runs the task on a background thread without blocking the caller, it could
   * return a {@link java.util.concurrent.Future} or a dummy value as the task
   * will completely asynchronously.
   *
   * @param event an {@link Event}, cannot be null
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

  public static <V> Trigger<V> synchronous(Consumer<Event> task) {
    return synchronous(e -> { task.accept(e); return null; });
  }

  public static <V> Trigger<V> asynchronous(Function<Event, V> task) {
    return new Trigger<>() {
      @Override
      public V run(Event event, Function<Task<V>, V> taskRunner) {
        return taskRunner.apply(new Task<>() {
          @Override
          protected V call() {
            return task.apply(event);
          }
        });
      }
    };
  }

  public static <V> Trigger<V> asynchronous(Consumer<Event> task) {
    return asynchronous(e -> { task.accept(e); return null; });
  }
}
