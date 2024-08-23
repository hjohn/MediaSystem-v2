package hs.mediasystem.ext.scanners;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class Paths {

  /**
   * Appends the relative path (which must be known to be a file) to a base URI
   * and returns the URI of the new path.<p>
   *
   * This avoids the known to be slow {@link Path#toUri()} method (it does a file system
   * call to check if the path indicates a file or a directory).
   *
   * @param base a base {@link URI}, cannot be {@code null}
   * @param relative a relative {@link Path} which must be a file, cannot be {@code null}
   * @return a new {@link URI}, never {@code null}
   */
  static URI appendFilePath(URI base, Path relative) {
    @SuppressWarnings("resource")
    String pathSeparator = relative.getFileSystem().getSeparator();
    String relativeString = relative.toString();

    if(!pathSeparator.equals("/")) {
      relativeString = relativeString.replace(pathSeparator, "/");
    }

    try {
      return base.resolve(new URI(null, null, relativeString, null));
    }
    catch(URISyntaxException e) {
      throw new IllegalStateException("Problem converting to URI: " + base + " + " + relative, e);
    }
  }
}
