package hs.mediasystem.util.javafx;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImageCacheTest {

  @Test
  public void shouldCalculateCorrectSize() {
    for(int i = 2; i < 1000; i += 2) {
      assertArrayEquals(new int[] {72, 108}, ImageCache.computeImageSize(i, i + i / 2, 108, 108));
      assertArrayEquals(new int[] {108, 72}, ImageCache.computeImageSize(i + i / 2, i, 108, 108));

      assertArrayEquals(new int[] {54, 81}, ImageCache.computeImageSize(i, i + i / 2, 54, 108));
      assertArrayEquals(new int[] {81, 54}, ImageCache.computeImageSize(i + i / 2, i, 108, 54));

      assertArrayEquals(new int[] {36, 54}, ImageCache.computeImageSize(i, i + i / 2, 108, 54));
      assertArrayEquals(new int[] {54, 36}, ImageCache.computeImageSize(i + i / 2, i, 54, 108));
    }

    Random rnd = new Random(1);

    for(int i = 0; i < 10000; i++) {
      int sw = rnd.nextInt(1000) + 1;
      int sh = rnd.nextInt(1000) + 1;
      int tw = rnd.nextInt(1000) + 1;
      int th = rnd.nextInt(1000) + 1;

      int[] result = ImageCache.computeImageSize(sw, sh, tw, th);
      String msg = String.format("src: %dx%d, target: %dx%d, result: %dx%d", sw, sh, tw, th, result[0], result[1]);

      assertTrue(result[0] <= tw, msg);
      assertTrue(result[1] <= th, msg);
      assertTrue(result[0] > 0, msg);
      assertTrue(result[1] > 0, msg);
      assertTrue(result[0] == tw || result[1] == th, msg);
    }
  }
}
