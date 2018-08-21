package hs.mediasystem.util.javafx.control;

import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class Containers {

  public static HBox hbox(String styleClass, Node... nodes) {
    HBox hbox = new HBox(nodes);

    if(styleClass != null) {
      hbox.getStyleClass().addAll(styleClass.split(",(?: *)"));
    }

    return hbox;
  }

  public static HBox hbox(Node... nodes) {
    return hbox(null, nodes);
  }

  public static VBox vbox(String styleClass, BooleanBinding visibility, Node... nodes) {
    VBox vbox = new VBox(nodes);

    if(styleClass != null) {
      vbox.getStyleClass().addAll(styleClass.split(",(?: *)"));
    }

    if(visibility != null) {
      vbox.managedProperty().bind(visibility);
      vbox.visibleProperty().bind(visibility);
    }

    return vbox;
  }

  public static VBox vbox(String styleClass, Node... nodes) {
    return vbox(styleClass, null, nodes);
  }

  public static VBox vbox(BooleanBinding visibility, Node... nodes) {
    return vbox(null, visibility, nodes);
  }

  public static VBox vbox(Node... nodes) {
    return vbox(null, null, nodes);
  }
}
