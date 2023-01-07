package hs.mediasystem.util.image;

import hs.mediasystem.util.domain.URLs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import javax.inject.Singleton;

@Singleton
public class HttpImageURIHandler implements ImageURIHandler {

  @Override
  public ImageHandle handle(ImageURI uri) {
    if(uri.getUri().startsWith("http:") || uri.getUri().startsWith("https:")) {
      return new HttpImageHandle(uri);
    }
    else if(uri.getUri().startsWith("file:")) {
      return new FileImageHandle(uri);
    }

    return null;
  }

  private class FileImageHandle implements ImageHandle {
    private final ImageURI uri;

    public FileImageHandle(ImageURI uri) {
      this.uri = uri;
    }

    @Override
    public byte[] getImageData() throws IOException {
      return Files.readAllBytes(Paths.get(uri.toURI()));
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

      FileImageHandle other = (FileImageHandle)obj;

      if(!uri.equals(other.uri)) {
        return false;
      }

      return true;
    }

    @Override
    public String toString() {
      return "FileImageHandle[" + uri + "]";
    }
  }

  private class HttpImageHandle implements ImageHandle {
    private final ImageURI uri;

    public HttpImageHandle(ImageURI uri) {
      this.uri = uri;
    }

    @Override
    public byte[] getImageData() throws IOException {
      return URLs.readAllBytes(uri.toURL(), uri.getKey() == null ? Map.of() : Map.of("!key", uri.getKey()));
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
