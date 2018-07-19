package hs.mediasystem.plugin.library.scene.serie;

import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;

public class SeasonBar extends HBox {

  @Override
  protected void layoutChildren() {
    double x = 0;

    setClip(new Rectangle(0, 0, getWidth(), getHeight()));

    ObservableList<Node> children = getChildren();
    for(int i = 0; i < children.size(); i++) {
      Label label = (Label)children.get(i);

      double w = label.prefWidth(-1);
      double h = label.prefHeight(-1);

      layoutInArea(label, x, 0, w, h, 0, null, HPos.CENTER, VPos.CENTER);

      x += w;
      x += getSpacing();
    }
  }
}
