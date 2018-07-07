package hs.mediasystem.util;

@FunctionalInterface
public interface TrySupplier<T> {
  T tryGet() throws Throwable;
}