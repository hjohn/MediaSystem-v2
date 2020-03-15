package hs.mediasystem.util.javafx.control.carousel;

import javafx.geometry.Dimension2D;
import javafx.geometry.Orientation;

public class LinearLayout extends Layout {

  @Override
  public CellIterator renderCellIterator(double fractionalIndex, Orientation orientation, Dimension2D size, int maxItems) {
    return new LinearCellIterator(this, fractionalIndex, orientation, size, maxItems);
  }

}
