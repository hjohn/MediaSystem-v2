package hs.mediasystem.util.javafx.beans;

public interface Accessor<T> {
  T read();
  void write(T value);
}
