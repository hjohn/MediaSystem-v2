package hs.mediasystem.ext.basicmediatypes.domain.stream;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface Scanner {

  /**
   * Performs a scan for the given root.<p>
   *
   * @param root a root to scan
   * @param importSourceId an import source id to associate with each result
   * @return a {@link List} of results, never null
   */
  List<Streamable> scan(Path root, int importSourceId) throws IOException;
}
