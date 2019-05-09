package hs.mediasystem.scanner.api;

import hs.mediasystem.util.StringURI;

import java.io.IOException;

public interface StreamPrintProvider {
  StreamPrint get(StringURI uri, Long size, long lastModificationTime) throws IOException;
  StreamPrint get(StreamID streamId);
}
