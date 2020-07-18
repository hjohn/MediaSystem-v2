package hs.mediasystem.util;

import java.io.IOException;
import java.io.InputStream;

public class ResourceImageHandle implements ImageHandle {
  private final Class<?> cls;
  private final String name;

  public ResourceImageHandle(Class<?> cls, String name) {
    this.cls = cls;
    this.name = name;
  }

  @Override
  public byte[] getImageData() throws IOException {
    try(InputStream stream = cls.getResourceAsStream(name)) {
      return stream.readAllBytes();
    }
  }

  @Override
  public String getKey() {
    return "classpath:/" + cls.getName() + ":" + name;
  }

  @Override
  public boolean isFastSource() {
    return true;
  }
}
