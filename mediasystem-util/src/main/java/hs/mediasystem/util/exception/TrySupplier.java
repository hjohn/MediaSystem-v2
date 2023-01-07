package hs.mediasystem.util.exception;

@FunctionalInterface
public interface TrySupplier<T> {
  T tryGet() throws Throwable;
}