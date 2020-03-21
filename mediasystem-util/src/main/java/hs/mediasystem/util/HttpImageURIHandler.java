package hs.mediasystem.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Objects;
import java.util.logging.Logger;

import javax.inject.Singleton;

@Singleton
public class HttpImageURIHandler implements ImageURIHandler {
  private static final Logger LOGGER = Logger.getLogger(HttpImageURIHandler.class.getName());

  @Override
  public ImageHandle handle(ImageURI uri) {
    if(uri.getUri().startsWith("http:") || uri.getUri().startsWith("https:") || uri.getUri().startsWith("file:")) {
      return new HttpImageHandle(uri.toURI());
    }

    return null;
  }

  private class HttpImageHandle implements ImageHandle {
    private final URI uri;

    public HttpImageHandle(URI uri) {
      this.uri = uri;
    }

    @Override
    public byte[] getImageData() {
      try {
        return URLs.readAllBytes(uri.toURL());
      }
      catch(RuntimeIOException | MalformedURLException e) {
        LOGGER.warning("RuntimeIOException while downloading image: " + uri + ": " + Throwables.formatAsOneLine(e));

        return null;
      }
    }

    @Override
    public String getKey() {
      return uri.toString();
    }

    @Override
    public boolean isFastSource() {
      return false;
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

      HttpImageHandle other = (HttpImageHandle)obj;

      if(!uri.equals(other.uri)) {
        return false;
      }

      return true;
    }

    @Override
    public String toString() {
      return "HttpImageHandle[" + uri + "]";
    }
  }
}
