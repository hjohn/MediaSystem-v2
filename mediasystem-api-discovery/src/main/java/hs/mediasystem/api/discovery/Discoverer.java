package hs.mediasystem.api.discovery;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.function.BiConsumer;

public interface Discoverer {

  /**
   * Scans a given {@link URI} for potential discoveries. Discoveries must
   * be provided in breadth first order (a parent must be discovered before any
   * of its children) or they will be ignored. If this order is violated, it
   * may take multiple passes before all discoveries are processed.
   *
   * @param root a root, cannot be {@code null}
   * @param consumer a call back to process a (partial) discovery hierarchy, cannot be {@code null}
   * @throws IOException when an I/O problem occurred
   */
  void discover(URI root, BiConsumer<URI, List<Discovery>> consumer) throws IOException;
}
