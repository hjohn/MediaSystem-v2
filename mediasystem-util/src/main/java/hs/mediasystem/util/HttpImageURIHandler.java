package hs.mediasystem.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import javax.inject.Singleton;

@Singleton
public class HttpImageURIHandler implements ImageURIHandler {

  @Override
  public ImageHandle handle(ImageURI uri) {
    if(uri.getUri().startsWith("http:") || uri.getUri().startsWith("https:")) {
      return new HttpImageHandle(uri.toURI());
    }
    else if(uri.getUri().startsWith("file:")) {
      return new FileImageHandle(uri.toURI());
    }

    return null;
  }

  private class FileImageHandle implements ImageHandle {
    private final URI uri;

    public FileImageHandle(URI uri) {
      this.uri = uri;
    }

    @Override
    public byte[] getImageData() throws IOException {
      return Files.readAllBytes(Paths.get(uri));
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
    private final URI uri;

    public HttpImageHandle(URI uri) {
      this.uri = uri;
    }

    @Override
    public byte[] getImageData() throws IOException {
      return URLs.readAllBytes(uri.toURL());
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
