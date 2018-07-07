package hs.mediasystem.util.javafx;

import hs.mediasystem.util.javafx.GridPane;

import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;

public class GridPaneUtil {
  public static void configure(javafx.scene.layout.GridPane gridPane, double[] columns, double[] rows) {
    for(double column : columns) {
      ColumnConstraints constraints = new ColumnConstraints();

      constraints.setPercentWidth(column);

      gridPane.getColumnConstraints().add(constraints);
    }

    for(double row : rows) {
      RowConstraints constraints = new RowConstraints();

      constraints.setPercentHeight(row);

      gridPane.getRowConstraints().add(constraints);
    }
  }

  public static GridPane create(double[] columns, double[] rows) {
    GridPane gridPane = new GridPane();

    configure(gridPane, columns, rows);

    return gridPane;
  }
}
