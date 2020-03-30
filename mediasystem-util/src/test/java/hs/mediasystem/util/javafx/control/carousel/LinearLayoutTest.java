package hs.mediasystem.util.javafx.control.carousel;

import javafx.geometry.Dimension2D;
import javafx.geometry.Orientation;
import javafx.scene.effect.PerspectiveTransform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LinearLayoutTest {
  private static final double DELTA = 0.01;

  @ParameterizedTest
  @ValueSource(doubles = {0.02, 0.01})
  public void testDensity(double density) {
    LinearLayout linearLayout = new LinearLayout();

    linearLayout.reflectionEnabledProperty().set(true);
    linearLayout.cellAlignmentProperty().set(0.5);
    linearLayout.densityProperty().set(density);

    CellIterator iterator = linearLayout.renderCellIterator(2, Orientation.HORIZONTAL, new Dimension2D(1920, 500), 4);
    SimpleCell[] cells = new SimpleCell[] {
      new SimpleCell(160, 90),
      new SimpleCell(160, 90),
      new SimpleCell(160, 90),
      new SimpleCell(160, 90)
    };

    PerspectiveTransform[] pts = new PerspectiveTransform[4];

    for(int index; (index = iterator.next()) >= 0; ) {
      pts[index] = iterator.calculate(index, new Dimension2D(cells[index].w, cells[index].h), 1.0, null).a;
    }

    // Density is the amount of cells to be displayed per pixel.  A density of 0.01 means to display
    // 1/100th of a cell per pixel, or in other words a cell is placed in a space of 100 pixels.

    double cellSpace = 1 / density;

    // Cells are 160 pixels wide; the center cell will be in the range of [-80, 80]

    verifyCell(pts[0], -80 + cellSpace * -2, -45, 160, 90);
    verifyCell(pts[1], -80 + cellSpace * -1, -45, 160, 90);
    verifyCell(pts[2], -80 + cellSpace * 0, -45, 160, 90);
    verifyCell(pts[3], -80 + cellSpace * 1, -45, 160, 90);
  }

  @Test
  public void testMaxCellSizes() {
    LinearLayout linearLayout = new LinearLayout();

    linearLayout.reflectionEnabledProperty().set(false);
    linearLayout.cellAlignmentProperty().set(0.5);
    linearLayout.densityProperty().set(0.01);
    linearLayout.maxCellWidthProperty().set(200);
    linearLayout.maxCellHeightProperty().set(200);

    CellIterator iterator = linearLayout.renderCellIterator(2, Orientation.HORIZONTAL, new Dimension2D(1920, 500), 4);
    SimpleCell[] cells = new SimpleCell[] {
      new SimpleCell(160, 90),
      new SimpleCell(160, 60),
      new SimpleCell(320, 90),
      new SimpleCell(100, 400)
    };

    PerspectiveTransform[] pts = new PerspectiveTransform[4];

    for(int index; (index = iterator.next()) >= 0; ) {
      pts[index] = iterator.calculate(index, new Dimension2D(cells[index].w, cells[index].h), 1.0, null).a;
    }

    int cellSpace = 100;

    verifyCell(pts[0], -80 + cellSpace * -2, -45, 160, 90);
    verifyCell(pts[1], -80 + cellSpace * -1, -30, 160, 60);
    verifyCell(pts[2], -100 + cellSpace * 0, -45.0 / 320 * 200, 200, 90.0 / 320 * 200);  // Cell scales with aspect ratio intact
    verifyCell(pts[3], -50.0 / 400 * 200 + cellSpace * 1, -100, 100.0 / 400 * 200, 200);
  }

  /**
   * Tests the height of the reflection.  To have a reflection, the
   * cell must be close enough to the horizon in the first place.
   */
  @Test
  public void testReflectionHeight() {
    LinearLayout linearLayout = new LinearLayout();

    linearLayout.reflectionEnabledProperty().set(true);
    linearLayout.cellAlignmentProperty().set(0.9);
    linearLayout.densityProperty().set(0.01);
    linearLayout.maxCellWidthProperty().set(200);
    linearLayout.maxCellHeightProperty().set(200);
    linearLayout.reflectionMaxHeight().set(47);
    linearLayout.reflectionHorizonDistance().set(3);

    CellIterator iterator = linearLayout.renderCellIterator(2, Orientation.HORIZONTAL, new Dimension2D(1920, 500), 4);
    SimpleCell[] cells = new SimpleCell[] {
      new SimpleCell(160, 180),
      new SimpleCell(160, 20),  // low height cell, gets shortened reflection
      new SimpleCell(160, 90),  // alignment places this at -1 y (200 height - 90 cell height = 110 -> 110 * 90% = 99 -> -100 + 99 = -1)
      new SimpleCell(100, 400)
    };

    PerspectiveTransform[] pts = new PerspectiveTransform[4];

    for(int index; (index = iterator.next()) >= 0; ) {
      pts[index] = iterator.calculate(index, new Dimension2D(cells[index].w, cells[index].h), 1.0, null).a;
    }

    int cellSpace = 100;

    verifyCell(pts[0], -80 + cellSpace * -2, -82, 160, 180 + 52);  // 1 distance from horizon (due to alignment, 10 pixels * 10%) * 2 + 3 extra distance + 47 reflection height = 52 additional height pixels
    verifyCell(pts[1], -80 + cellSpace * -1, 62, 160, 20 + 62);  // 9 distance from horizon (due to alignment, 90 pixels * 10%) * 2 + 3 extra distance + min(47, 2 * 9 + 3 + 20) reflection height = 62 additonal height pixels (cell height of 20 limits reflection height)
    verifyCell(pts[2], -80 + cellSpace * 0, -1.0, 160, 90 + 61);  // 5.5 distance from horizon (due to alignment, 55 pixels * 10%) * 2 + 3 extra distance + 47 reflection height = 61 additional height pixels
    verifyCell(pts[3], -50.0 / 400 * 200 + cellSpace * 1, -100, 100.0 / 400 * 200, 200 + 50);  // 47 reflection height + 3 distance = 50 additional height pixels
  }

  @Test
  public void testVertical() {
    LinearLayout linearLayout = new LinearLayout();

    linearLayout.reflectionEnabledProperty().set(true);  // ignored when vertical
    linearLayout.cellAlignmentProperty().set(0.5);
    linearLayout.densityProperty().set(0.01);
    linearLayout.maxCellWidthProperty().set(200);
    linearLayout.maxCellHeightProperty().set(200);

    CellIterator iterator = linearLayout.renderCellIterator(2, Orientation.VERTICAL, new Dimension2D(600, 1200), 4);
    SimpleCell[] cells = new SimpleCell[] {
      new SimpleCell(160, 90),
      new SimpleCell(160, 60),
      new SimpleCell(320, 90),
      new SimpleCell(100, 400)
    };

    PerspectiveTransform[] pts = new PerspectiveTransform[4];

    for(int index; (index = iterator.next()) >= 0; ) {
      pts[index] = iterator.calculate(index, new Dimension2D(cells[index].w, cells[index].h), 1.0, null).a;
    }

    verifyCell(pts[0], -80, -245, 160, 90);
    verifyCell(pts[1], -80, -130, 160, 60);
    verifyCell(pts[2], -100, -45.0 / 320 * 200, 200, 90.0 / 320 * 200);  // Cell scales with aspect ratio intact
    verifyCell(pts[3], -25, 0, 100.0 / 400 * 200, 200);
  }

  private static void verifyCell(PerspectiveTransform pt, double ulx, double uly, double w, double h) {
    assertEquals(h, pt.getLry() - pt.getUry(), "height");
    assertEquals(ulx, pt.getUlx(), DELTA, "upper left x");
    assertEquals(uly, pt.getUly(), DELTA, "upper left y");
    assertEquals(ulx + w, pt.getUrx(), DELTA);
    assertEquals(uly, pt.getUry(), DELTA);
    assertEquals(ulx, pt.getLlx(), DELTA);
    assertEquals(uly + h, pt.getLly(), DELTA, "lower left y");
    assertEquals(ulx + w, pt.getLrx(), DELTA);
    assertEquals(uly + h, pt.getLry(), DELTA);
  }

  private static class SimpleCell {
    private final int w;
    private final int h;

    public SimpleCell(int w, int h) {
      this.w = w;
      this.h = h;
    }
  }
}
