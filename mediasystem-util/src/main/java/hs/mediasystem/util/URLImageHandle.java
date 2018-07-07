package hs.mediasystem.util;

import java.net.URL;
import java.util.Objects;
import java.util.logging.Logger;

public class URLImageHandle implements ImageHandle {
  private static final Logger LOGGER = Logger.getLogger(URLImageHandle.class.getName());

  private final URL url;

  public URLImageHandle(URL url) {
    if(url == null) {
      throw new IllegalArgumentException("url cannot be null");
    }

    this.url = url;
  }

  @Override
  public byte[] getImageData() {
    try {
      LOGGER.fine("Downloading '" + url + "'");

      return URLs.readAllBytes(url);
    }
    catch(RuntimeIOException e) {
      return null;
    }
  }

  @Override
  public String getKey() {
    return url.toExternalForm();
  }

  @Override
  public boolean isFastSource() {
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(url);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    URLImageHandle other = (URLImageHandle)obj;

    if(!url.equals(other.url)) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return "ImageHandle[" + url + "]";
  }
}
