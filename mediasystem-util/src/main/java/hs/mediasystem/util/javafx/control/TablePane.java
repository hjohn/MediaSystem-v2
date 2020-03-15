package hs.mediasystem.util.javafx.control;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;

public class TablePane extends GridPane {
  private int row = 0;
  private int column = 0;

  public TablePane add(Node node) {
    getChildren().add(node);
    GridPane.setConstraints(node, column++, row);
    return this;
  }

  public TablePane add(Node node, int columnSpan) {
    getChildren().add(node);
    GridPane.setConstraints(node, column, row, columnSpan, 1);
    column += columnSpan;
    return this;
  }

  public TablePane nextColumn() {
    column++;
    return this;
  }

  public TablePane nextRow() {
    column = 0;
    row++;
    return this;
  }
}
