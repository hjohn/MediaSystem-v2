package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.util.Exceptional;

import java.nio.file.Path;
import java.util.List;

public interface Scanner {

  /**
   * Performs a scan for the given roots.  Returns a {@link List} with one entry per given
   * root.<p>
   *
   * Each entry is a {@link List} of results found, wrapped in an {@link Exceptional}.  The
   * Exceptional can signal problems with the given root.
   *
   * @param roots a set of roots to scan
   * @param tags a set of tags to associate with each result
   * @return a {@link List} per root of {@link Exceptional} lists, never null and
   *         always contains the same number of entries as the number of roots passed in.
   */
  List<Exceptional<List<Streamable>>> scan(List<Path> roots);
}
