package hs.mediasystem.domain.media;

public record Resolution(int width, int height, Float pixelAspectRatio) implements Comparable<Resolution> {

  @Override
  public int compareTo(Resolution o) {
    int c = Long.signum((long)width * height - (long)o.width * o.height);

    if(c != 0) {
      return c;
    }

    if(pixelAspectRatio == null) {
      return o.pixelAspectRatio == null ? 0 : -1;
    }

    return Float.compare(pixelAspectRatio, o.pixelAspectRatio);
  }
}
