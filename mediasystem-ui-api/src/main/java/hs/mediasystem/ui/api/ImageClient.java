package hs.mediasystem.ui.api;

import java.util.Optional;

public interface ImageClient {
  Optional<byte[]> findImage(String id);
}
