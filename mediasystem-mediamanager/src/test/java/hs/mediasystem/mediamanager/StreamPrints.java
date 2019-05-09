package hs.mediasystem.mediamanager;

import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.scanner.api.StreamPrint;

public class StreamPrints {
  public static StreamPrint create() {
    return create(new StreamID(777));
  }

  public static StreamPrint create(StreamID id) {
    return new StreamPrint(id, 1000000L, 12345, new byte[] {1, 2, 3, 4, 5});
  }

}
