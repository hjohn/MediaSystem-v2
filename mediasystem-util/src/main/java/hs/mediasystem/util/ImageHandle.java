package hs.mediasystem.util;

public interface ImageHandle {
  byte[] getImageData();
  String getKey();
  boolean isFastSource();
}
