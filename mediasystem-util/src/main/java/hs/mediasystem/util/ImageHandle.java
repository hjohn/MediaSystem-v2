package hs.mediasystem.util;

import java.io.IOException;

public interface ImageHandle {

  /**
   * Returns the image data as a byte array.
   *
   * @return a byte array containing the image data, never null
   * @throws IOException when the data could not be obtained
   */
  byte[] getImageData() throws IOException;

  String getKey();
  boolean isFastSource();
}
