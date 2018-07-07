package hs.mediasystem.util.javafx;

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

  public static VBox vbox(String styleClass, Node... nodes) {
    VBox vbox = new VBox(nodes);

    if(styleClass != null) {
      vbox.getStyleClass().addAll(styleClass.split(",(?: *)"));
    }

    return vbox;
  }

  public static VBox vbox(Node... nodes) {
    return vbox(null, nodes);
  }
}
