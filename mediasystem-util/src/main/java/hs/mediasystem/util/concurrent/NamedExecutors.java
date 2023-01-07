package hs.mediasystem.util.concurrent;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NamedExecutors {

  /**
   * Creates an executor which executes a maximum of one task concurrently and silently
   * rejects all tasks when a task is running.
   *
   * @param name base name of the threads created, cannot be {@code null}
   * @return an {@link Executor}, never {@code null}
   */
  public static Executor newSingleTaskExecutor(String name) {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(
      1, 1, 30, TimeUnit.SECONDS,
      new SynchronousQueue<>(),
      new NamedThreadFactory(Objects.requireNonNull(name, "name cannot be null"), true),
      new ThreadPoolExecutor.DiscardPolicy()
    );

    executor.allowCoreThreadTimeOut(true);

    return executor;
  }
}
