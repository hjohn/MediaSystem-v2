package hs.mediasystem.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Objects;

public class StringURI implements Comparable<StringURI> {
  private final String uri;

  public StringURI(String uri) {
    URI.create(uri);

    this.uri = uri;
  }

  public StringURI(URI uri) {
    this.uri = uri.toString();
  }

  public String asReadableString() {
    try {
      return URLDecoder.decode(uri, "UTF-8");
    }
    catch(UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  public String getUri() {
    return uri;
  }

  public URI toURI() {
    return URI.create(uri);
  }

  public URL toURL() throws MalformedURLException {
    return toURI().toURL();
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    StringURI other = (StringURI)obj;

    if(!uri.equals(other.uri)) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return uri;
  }

  @Override
  public int compareTo(StringURI o) {
    return uri.compareTo(o.uri);
  }
}
