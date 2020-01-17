package hs.mediasystem.ext.vlc.util;

public interface Accessor<T> {
  T read();
  void write(T value);
}
