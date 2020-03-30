package hs.mediasystem.util;

import java.io.IOException;

public interface ImageHandle {
  byte[] getImageData() throws IOException;
  String getKey();
  boolean isFastSource();
}
