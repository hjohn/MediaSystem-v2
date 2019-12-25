package hs.mediasystem.runner.util;

import hs.database.core.DatabaseException;
import hs.mediasystem.mediamanager.StreamMetaDataStore;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.ImageURIHandler;
import hs.mediasystem.util.Throwables;

import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalImageURIHandler implements ImageURIHandler {
  private static final Logger LOGGER = Logger.getLogger(LocalImageURIHandler.class.getName());
  private static final Pattern PATTERN = Pattern.compile("(?i)localdb://([0-9]+)/([0-9]+)");

  @Inject private StreamMetaDataStore provider;

  @Override
  public ImageHandle handle(ImageURI uri) {
    if(uri.getUri().startsWith("localdb:")) {
      return new LocalImageHandle(uri);
    }

    return null;
  }

  private class LocalImageHandle implements ImageHandle {
    private final ImageURI uri;

    public LocalImageHandle(ImageURI uri) {
      this.uri = uri;
    }

    @Override
    public byte[] getImageData() {
      try {
        Matcher matcher = PATTERN.matcher(uri.getUri());

        if(!matcher.matches()) {
          throw new IllegalArgumentException("Invalid localdb uri: " + uri);
        }

        return provider.readSnapshot(new StreamID(Integer.parseInt(matcher.group(1))), Integer.parseInt(matcher.group(2)));
      }
      catch(DatabaseException e) {
        LOGGER.warning("Exception while reading image: " + uri + ": " + Throwables.formatAsOneLine(e));

        return null;
      }
    }

    @Override
    public String getKey() {
      return uri.toString();
    }

    @Override
    public boolean isFastSource() {
      return true;
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

      LocalImageHandle other = (LocalImageHandle)obj;

      if(!uri.equals(other.uri)) {
        return false;
      }

      return true;
    }

    @Override
    public String toString() {
      return "LocalImageHandle[" + uri + "]";
    }
  }
}
