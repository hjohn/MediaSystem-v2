package hs.mediasystem.api.discovery;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public interface Discoverer {

  interface Registry {

    /**
     * Register a list of discoveries for a location.
     *
     * @param parentLocation a parent location, cannot be {@code null}
     * @param discoveries a list of discoveries, cannot be {@code null}, but can be empty
     */
    void register(URI parentLocation, List<Discovery> discoveries);
  }

  /**
   * Scans a given {@link URI} for potential discoveries. Discoveries must
   * be provided in breadth first order (a parent must be discovered before any
   * of its children) or they will be ignored. If this order is violated, it
   * may take multiple passes before all discoveries are processed.
   *
   * @param root a root, cannot be {@code null}
   * @param registry a place to register discoveries, cannot be {@code null}
   * @throws IOException when an I/O problem occurred
   */
  void discover(URI root, Registry registry) throws IOException;
}
