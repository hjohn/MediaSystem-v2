package hs.mediasystem.util.exception;

@FunctionalInterface
public interface TryRunnable {
  void run() throws Throwable;
}