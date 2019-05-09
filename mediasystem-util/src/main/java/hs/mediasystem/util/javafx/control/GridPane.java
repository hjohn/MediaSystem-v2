package hs.mediasystem.util.javafx.control;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class GridPane extends javafx.scene.layout.GridPane {
  public static final Node FILL = new Pane();

  private int row;
  private int column;

  /**
   * Adds a Node at the given position.
   *
   * @param col a column
   * @param row a row
   * @return fluent {@link Adder}
   */
  public Adder at(int col, int row) {
    return new Adder(col, row);
  }

  /**
   * Adds a Node at the given column, in the current row.
   *
   * @param col a column
   * @return fluent {@link Adder}
   */
  public Adder at(int column) {
    this.column = column + 1;

    return new Adder(column, row);
  }

  /**
   * Adds a Node at the current position.
   *
   * @return fluent {@link Adder}
   */
  public Adder at() {
    return new Adder(column++, row);
  }

  public void add(Node node) {
    if(node != null) {
      add(node, column, row);
    }
    column++;
  }

  public void setColumn(int column) {
    this.column = column;
  }

  public void addRow(Node... nodes) {
    for(int i = 0; i < nodes.length; i++) {
      Node node = nodes[i];
      int colSpan = 1;

      if(node != null) {
        for(int j = i + 1; j < nodes.length; j++) {
          if(nodes[j] != GridPane.FILL) {
            break;
          }

          colSpan++;
          i++;
        }
        add(node, column, row, colSpan, 1);
      }
      column += colSpan;
    }

    nextRow();
  }

  public void nextRow() {
    row++;
    column = 0;
  }

  public class Adder {
    private final int col;
    private final int row;

    private int colSpan = 1;
    private int rowSpan = 1;
    private boolean fillWidth = false;
    private boolean fillHeight = false;
    private HPos hpos;
    private VPos vpos;
    private String[] styleClasses;

    private Adder(int col, int row) {
      this.col = col;
      this.row = row;
    }

    public Adder spanning(int cols, int rows) {
      this.colSpan = cols;
      this.rowSpan = rows;
      return this;
    }

    public Adder fillWidth() {
      this.fillWidth = true;
      return this;
    }

    public Adder fillHeight() {
      this.fillHeight = true;
      return this;
    }

    public Adder align(HPos hpos) {
      this.hpos = hpos;
      return this;
    }

    public Adder align(VPos vpos) {
      this.vpos = vpos;
      return this;
    }

    public Adder styleClass(String... styleClasses) {
      this.styleClasses = styleClasses;
      return this;
    }

    public void add(Node node) {
      if(node != null) {
        GridPane.this.add(node, col, row, colSpan, rowSpan);

        if(fillWidth) {
          javafx.scene.layout.GridPane.setFillWidth(node, true);
        }
        if(fillHeight) {
          javafx.scene.layout.GridPane.setFillHeight(node, true);
        }
        if(hpos != null) {
          javafx.scene.layout.GridPane.setHalignment(node, hpos);
        }
        if(vpos != null) {
          javafx.scene.layout.GridPane.setValignment(node, vpos);
        }
        if(styleClasses != null) {
          node.getStyleClass().addAll(styleClasses);
        }
      }
      else {
        GridPane.this.setColumn(col + colSpan);
      }
    }
  }
}
