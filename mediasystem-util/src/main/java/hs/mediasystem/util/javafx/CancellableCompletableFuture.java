package hs.mediasystem.util.javafx;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;

/**
 * This subclass is specifically created to allow for cancelling of upstream
 * futures if all downstream futures were cancelled (this has some limitations
 * and won't work when sharing the future and allowing arbitrary downstream
 * operations -- which is why this is not a generic useable class).<p>
 *
 * Since CompletableFuture's themselves can not interrupt their worker threads
 * (because the future does not know which thread), a FutureTask is used
 * internally (for the first CompletableFuture only), which can be interrupted.<p>
 *
 * Use case: if the same image is already being loaded, a 2nd caller should just
 * wait until that one is finished.  If the first caller is cancelled, but not
 * the 2nd, it should still complete.  If both are cancelled the loading can
 * be cancelled.
 */
class CancellableCompletableFuture<T> extends CompletableFuture<T> {
  private final Set<CompletableFuture<?>> notCancelledDependents = Collections.synchronizedSet(new HashSet<>());
  private final CancellableCompletableFuture<?> parent;
  private final FutureTask<?> associatedTask;

  public CancellableCompletableFuture(Consumer<CancellableCompletableFuture<T>> function, Executor executor) {
    this.parent = null;
    this.associatedTask = new FutureTask<>(() -> function.accept(this), null);

    executor.execute(associatedTask);
  }

  private CancellableCompletableFuture(CancellableCompletableFuture<?> parent) {
    this.parent = parent;
    this.associatedTask = null;
  }

  @Override
  public <U> CompletableFuture<U> newIncompleteFuture() {
    CancellableCompletableFuture<U> cf = new CancellableCompletableFuture<>(this);

    notCancelledDependents.add(cf);

    return cf;
  }

  private void cancel(CompletableFuture<?> child) {
    notCancelledDependents.remove(child);

    if(notCancelledDependents.isEmpty()) {
      if(associatedTask != null) {
        associatedTask.cancel(true);
      }

      cancel(true);
    }
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    super.cancel(mayInterruptIfRunning);

    if(parent != null) {
      parent.cancel(this);  // Inform parent that one of its children was cancelled
    }

    return true;
  }
}