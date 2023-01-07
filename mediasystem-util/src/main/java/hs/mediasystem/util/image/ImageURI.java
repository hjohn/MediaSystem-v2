package hs.mediasystem.util.image;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

public class ImageURI implements Comparable<ImageURI> {
  private final String uri;
  private final String key;

  public ImageURI(String uri, String key) {
    this.uri = uri;
    this.key = key;
  }

  public String getKey() {
    return key;
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
  public int compareTo(ImageURI o) {
    return uri.compareTo(o.uri);
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

    ImageURI other = (ImageURI)obj;

    if(!uri.equals(other.uri)) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return uri;
  }
}
