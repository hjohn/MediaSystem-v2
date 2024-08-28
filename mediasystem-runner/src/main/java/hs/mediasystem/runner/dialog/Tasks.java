package hs.mediasystem.runner.dialog;

import java.util.function.Supplier;

import javafx.concurrent.Task;

public class Tasks {
  public static <T> Task<T> of(Supplier<T> supplier) {
    return new Task<>() {
      @Override
      protected T call() {
        updateTitle("Loading...");

        return supplier.get();
      }
    };
  }
}
