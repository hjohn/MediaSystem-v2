package hs.mediasystem.util.javafx.control;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;

public class GridPane extends javafx.scene.layout.GridPane {

  public Adder at(int col, int row) {
    return new Adder(col, row);
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
  }
}
