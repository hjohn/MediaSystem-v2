package hs.mediasystem.util.javafx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class CancellableCompletableFutureTest {

  @Test
  void shouldCancelParentTaskWhenAllChildrenCancelled() {
    ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);

    // Keep threads of executor busy, so it cannot start the task associated with the root future:
    for(int i = 0; i < 5; i++) {
      EXECUTOR.execute(() -> {
        try {
          Thread.sleep(1000);
        }
        catch(InterruptedException e) {
          e.printStackTrace();
        }
      });
    }

    CancellableCompletableFuture<Object> root = new CancellableCompletableFuture<>(cf -> { fail("Shouldn't get here"); }, EXECUTOR);
    CompletableFuture<Void> child1 = root.thenApply(x -> "" + x).thenAcceptAsync(x -> {});
    CompletableFuture<Void> child2 = root.thenAcceptAsync(x -> {}).exceptionally(e -> { return null; });

    child1.cancel(true);

    // Check child future is cancelled and done:
    assertTrue(child1.isDone());
    assertTrue(child1.isCancelled());

    // Check that root future is not cancelled or done:
    assertFalse(root.isDone());
    assertFalse(root.isCancelled());

    // Now also cancel the 2nd dependent:
    child2.cancel(true);

    // Verify 2nd dependent is cancelled and done:
    assertTrue(child2.isDone());
    assertTrue(child2.isCancelled());

    // Verify root future is now also cancelled and done:
    assertTrue(root.isDone());
    assertTrue(root.isCancelled());
  }

}
