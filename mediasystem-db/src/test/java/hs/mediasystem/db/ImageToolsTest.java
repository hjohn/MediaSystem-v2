/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package hs.mediasystem.db;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImageToolsTest {
    private static final int RANDOM_SEED = 1;  // A random seed
    private static final int MIN_SIZE = 1;  // Smallest dimension to generate
    private static final int MAX_SIZE = 1000;  // Largest dimension to generate
    private static final int RANGE = MAX_SIZE - MIN_SIZE;  // Generate dimension range
    private static final int RANDOM_COUNT = 1000;  // Number of random source and target dimensions to generate and verify

    private final Random rnd = new Random(RANDOM_SEED);

    @Test
    public void testIfComputeDimensionsReturnsValuesInCorrectRangeWhenAspectRatioIsPreserved() {
        // Test a few specific corner cases:
        assertComputeDimensions(1000, 1500, 108, 108);
        assertComputeDimensions(800, 1200, 108, 108);
        assertComputeDimensions(1400, 2100, 108, 108);
        assertComputeDimensions(2000, 3000, 108, 108);
        assertComputeDimensions(98, 97, 40, 50);
        assertComputeDimensions(98, 97, 40, 0);
        assertComputeDimensions(98, 97, 0, 50);
        assertComputeDimensions(98, 97, 0, 0);
        assertComputeDimensions(98, 97, -1, -1);
        assertComputeDimensions(98, 97, 98, 97);
        assertComputeDimensions(98, 6, 3, 3);

        // Test a few random values:
        for (int i = 0; i < RANDOM_COUNT; i++) {
            int sw = rnd.nextInt(RANGE) + MIN_SIZE;
            int sh = rnd.nextInt(RANGE) + MIN_SIZE;
            int tw = rnd.nextInt(RANGE) + MIN_SIZE;
            int th = rnd.nextInt(RANGE) + MIN_SIZE;

            assertComputeDimensions(sw, sh, tw, th);
        }
    }

    @Test
    public void testIfComputeDimensionsReturnsValuesInCorrectRangeWhenAspectRatioIsNotPreserved() {
        assertArrayEquals(new int[] {10, 15}, computeDimensions(100, 101, 10, 15, false));
        assertArrayEquals(new int[] {100, 15}, computeDimensions(100, 101, 0, 15, false));
        assertArrayEquals(new int[] {100, 101}, computeDimensions(100, 101, 0, 0, false));
        assertArrayEquals(new int[] {10, 101}, computeDimensions(100, 101, 10, 0, false));
        assertArrayEquals(new int[] {100, 101}, computeDimensions(100, 101, -1, 0, false));
        assertArrayEquals(new int[] {100, 101}, computeDimensions(100, 101, -1, -1, false));
        assertArrayEquals(new int[] {100, 101}, computeDimensions(100, 101, 0, -1, false));
    }

    private static void assertComputeDimensions(int sw, int sh, int tw, int th) {
        int[] result = computeDimensions(sw, sh, tw, th, true);

        int x = result[0];
        int y = result[1];
        double originalAspect = (double)sw / sh;

        String msg = String.format("src: %dx%d, target: %dx%d, result: %dx%d", sw, sh, tw, th, x, y);

        // When any target dimension is 0 or negative, it defaults to the source dimension size
        tw = tw <= 0 ? sw : tw;
        th = th <= 0 ? sh : th;

        // Result should always return dimensions in the range of 0 < x <= size,
        // and one dimension must be equal to one of the target dimensions.
        assertTrue(x <= tw, msg);
        assertTrue(y <= th, msg);
        assertTrue(x > 0, msg);
        assertTrue(y > 0, msg);
        assertTrue(x == tw || y == th, msg);

        // Check the non-maxed dimension to see if it is within 1 pixel of the expected value
        // when calculated with the original aspect ratio:
        if (x != tw) {
            assertTrue(x == Math.floor(th * originalAspect) || x == Math.ceil(th * originalAspect), msg);
        }
        if (y != th) {
            assertTrue(y == Math.floor(tw / originalAspect) || y == Math.ceil(tw / originalAspect), msg);
        }
    }

  public static int[] computeDimensions(int sourceWidth, int sourceHeight,
    int maxWidth, int maxHeight, boolean preserveAspectRatio) {
    // ensure non-negative dimensions (0 implies default)
    int finalWidth = maxWidth < 0 ? 0 : maxWidth;
    int finalHeight = maxHeight < 0 ? 0 : maxHeight;

    if(finalWidth == 0 && finalHeight == 0) {
      // default to source dimensions
      finalWidth = sourceWidth;
      finalHeight = sourceHeight;
    }
    else if(finalWidth != sourceWidth || finalHeight != sourceHeight) {
      if(preserveAspectRatio) {
        // compute the final dimensions
        if(finalWidth == 0) {
          finalWidth = Math.round((float)sourceWidth * finalHeight / sourceHeight);
        }
        else if(finalHeight == 0) {
          finalHeight = Math.round((float)sourceHeight * finalWidth / sourceWidth);
        }
        else {
          float scale = Math.min((float)finalWidth / sourceWidth, (float)finalHeight / sourceHeight);
          finalWidth = Math.round(sourceWidth * scale);
          finalHeight = Math.round(sourceHeight * scale);
        }
      }
      else {
        // set final dimensions to default if zero
        if(finalHeight == 0) {
          finalHeight = sourceHeight;
        }
        if(finalWidth == 0) {
          finalWidth = sourceWidth;
        }
      }

      // clamp dimensions to positive values
      if(finalWidth == 0) {
        finalWidth = 1;
      }
      if(finalHeight == 0) {
        finalHeight = 1;
      }
    }

    return new int[] {finalWidth, finalHeight};
  }
}
