package hs.mediasystem.ext.tmdb.provider;

import java.io.IOException;
import java.util.Optional;

public interface MediaProvider<T> {

  /**
   * Given a TMDB key, provides a specific TMDB type. If the key is unknown
   * or not found, returns empty. If there is an IO error, throws {@link IOException}.
   *
   * @param key a TMDB key, cannot be {@code null}
   * @return an optional result, never {@code null}
   * @throws IOException when an I/O problem occurred
   */
  Optional<T> provide(String key) throws IOException;
}
