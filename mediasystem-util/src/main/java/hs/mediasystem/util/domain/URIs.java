package hs.mediasystem.util.domain;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utility methods for working with {@link URI}s.
 */
public class URIs {

  /**
   * Ensures that the toString representation of a URI is normalized. URI's
   * sometimes are written with a triple slash when it is not needed, and
   * other constructions may omit the triple slash. When converting to strings
   * this would result in strings which are not equal.
   *
   * @param uri a {@link URI} to normalize, cannot be {@code null}
   * @return a normalized {@link URI}, never {@code null}
   */
  public static URI normalize(URI uri) {
    String path = uri.getPath();
    String lastPart = path.substring(path.lastIndexOf('/') + 1);

    return uri.resolve(lastPart).normalize();
  }

  /**
   * Ensures that the toString representation of a URI is normalized and
   * that its path part ends with a slash. URI's sometimes are written with a
   * triple slash when it is not needed, and other constructions may omit the
   * triple slash. When converting to strings this would result in strings
   * which are not equal.
   *
   * @param uri a {@link URI} to normalize, cannot be {@code null}
   * @return a normalized {@link URI}, never {@code null}
   */
  public static URI normalizeAsFolder(URI uri) {
    String s = uri.getPath();

    if(!s.endsWith("/")) {
      s += "/";
    }

    try {
      return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), s, uri.getQuery(), uri.getFragment());
    }
    catch(URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Ensures that the toString representation of a URI is normalized and
   * that its path part does not end with a slash. URI's sometimes are written with a
   * triple slash when it is not needed, and other constructions may omit the
   * triple slash. When converting to strings this would result in strings
   * which are not equal.
   *
   * @param uri a {@link URI} to normalize, cannot be {@code null}
   * @return a normalized {@link URI}, never {@code null}
   */
  public static URI normalizeAsFile(URI uri) {
    String s = uri.getPath();

    if(s.endsWith("/")) {
      s = s.substring(0, s.length() - 1);
    }

    try {
      return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), s, uri.getQuery(), uri.getFragment());
    }
    catch(URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Checks if the given ancestor URI is actually an ancestor of the given child URI.
   * A URI is not considered an ancestor of itself.
   *
   * @param ancestor an ancestor URI, cannot be {@code null}
   * @param child a child URI, cannot be {@code null}
   * @return {@code true} if the child URI has the ancestor URI as an ancestor, otherwise {@code false}
   * @throws NullPointerException when ancestor or child are {@code null}
   */
  public static boolean isAncestorOf(URI ancestor, URI child) {
    if(!child.resolve(ancestor.getPath()).equals(ancestor)) {  // check if the URI's are related at all
      return false;
    }

    String path = ancestor.getPath();
    String childPath = child.getPath();

    return path.endsWith("/") ? childPath.startsWith(path) && !childPath.equals(path) : childPath.startsWith(path + "/");
  }
}
