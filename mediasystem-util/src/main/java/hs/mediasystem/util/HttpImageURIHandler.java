package hs.mediasystem.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Logger;

import javax.inject.Singleton;

@Singleton
public class HttpImageURIHandler implements ImageURIHandler {
  private static final Logger LOGGER = Logger.getLogger(HttpImageURIHandler.class.getName());

  @Override
  public ImageHandle handle(ImageURI uri) {
    if(uri.getUri().startsWith("http:") || uri.getUri().startsWith("https:")) {
      try {
        return new HttpImageHandle(uri.toURL());
      }
      catch(MalformedURLException e) {
        throw new IllegalStateException(e);
      }
    }

    return null;
  }

  private class HttpImageHandle implements ImageHandle {
    private final URL url;

    public HttpImageHandle(URL url) {
      this.url = url;
    }

    @Override
    public byte[] getImageData() {
      try {
        return URLs.readAllBytes(url);
      }
      catch(RuntimeIOException e) {
        LOGGER.warning("RuntimeIOException while downloading image: " + url + ": " + Throwables.formatAsOneLine(e));

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

      HttpImageHandle other = (HttpImageHandle)obj;

      if(!url.equals(other.url)) {
        return false;
      }

      return true;
    }

    @Override
    public String toString() {
      return "HttpImageHandle[" + url + "]";
    }
  }
}
