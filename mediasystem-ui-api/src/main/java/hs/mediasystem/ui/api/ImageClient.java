package hs.mediasystem.ui.api;

import java.io.IOException;
import java.util.Optional;

public interface ImageClient {
  Optional<byte[]> findImage(String id) throws IOException;
}
