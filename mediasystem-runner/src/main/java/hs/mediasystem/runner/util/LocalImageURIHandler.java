package hs.mediasystem.runner.util;

import hs.mediasystem.ui.api.ImageClient;
import hs.mediasystem.util.image.ImageHandle;
import hs.mediasystem.util.image.ImageURI;
import hs.mediasystem.util.image.ImageURIHandler;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalImageURIHandler implements ImageURIHandler {
  private static final Pattern PATTERN = Pattern.compile("(?i)localdb://([0-9]+)/([0-9]+)");

  @Inject private ImageClient imageClient;

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
    public byte[] getImageData() throws IOException {
      Matcher matcher = PATTERN.matcher(uri.getUri());

      if(!matcher.matches()) {
        throw new IllegalArgumentException("Invalid localdb uri: " + uri);
      }

      return imageClient.findImage(matcher.group(1) + ":" + matcher.group(2)).orElseThrow(() -> new IOException("No image available in database for: " + uri));
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
