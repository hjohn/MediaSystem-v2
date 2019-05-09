package hs.mediasystem.ext.basicmediatypes.domain.stream;

public class Resolution {
  private final int width;
  private final int height;
  private final Float pixelAspectRatio;

  public Resolution(int width, int height, Float pixelAspectRatio) {
    this.width = width;
    this.height = height;
    this.pixelAspectRatio = pixelAspectRatio;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public Float getPixelAspectRatio() {
    return pixelAspectRatio;
  }
}
