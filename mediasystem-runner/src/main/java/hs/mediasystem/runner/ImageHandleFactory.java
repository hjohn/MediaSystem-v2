package hs.mediasystem.runner;

import hs.database.core.DatabaseException;
import hs.mediasystem.db.ImageStore;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.RuntimeIOException;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.URLs;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ImageHandleFactory {
  private static final Logger LOGGER = Logger.getLogger(ImageHandleFactory.class.getName());

  @Inject private ImageStore store;

  public ImageHandle fromURI(ImageURI uri) {
    if(uri == null) {
      throw new IllegalArgumentException("uri cannot be null");
    }

    try {
      return new DatabaseImageHandle(uri.toURL());
    }
    catch(MalformedURLException e) {
      throw new IllegalStateException(e);
    }
  }

  public ImageHandle fromURL(URL url) {
    if(url == null) {
      throw new IllegalArgumentException("url cannot be null");
    }

    return new DatabaseImageHandle(url);
  }

  private class DatabaseImageHandle implements ImageHandle {
    private final URL url;

    public DatabaseImageHandle(URL url) {
      this.url = url;
    }

    @Override
    public byte[] getImageData() {
      try {
        byte[] data = store.findByURL(url);

        if(data != null) {
          return data;
        }
      }
      catch(DatabaseException e) {
        LOGGER.warning("DatabaseException while loading image: " + url + ": " + Throwables.formatAsOneLine(e));
      }

      try {
        byte[] data = URLs.readAllBytes(url);

        try {
          store.store(url, data);
        }
        catch(DatabaseException e) {
          LOGGER.warning("DatabaseException while storing image: " + url + ": " + Throwables.formatAsOneLine(e));
        }

        return data;
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

      DatabaseImageHandle other = (DatabaseImageHandle)obj;

      if(!url.equals(other.url)) {
        return false;
      }

      return true;
    }

    @Override
    public String toString() {
      return "DatabaseImageHandle[" + url + "]";
    }
  }
}
