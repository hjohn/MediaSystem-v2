package hs.mediasystem.util.javafx.control.csslayout;

import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;

public class StylableContainers {

  public static StylableHBox hbox(String styleClass, Node... nodes) {
    StylableHBox hbox = new StylableHBox(nodes);

    addStyleClass(hbox, styleClass);

    return hbox;
  }

  public static StylableHBox hbox(Node... nodes) {
    return hbox(null, nodes);
  }

  public static StylableVBox vbox(String styleClass, BooleanBinding visibility, Node... nodes) {
    StylableVBox vbox = new StylableVBox(nodes);

    addStyleClass(vbox, styleClass);

    if(visibility != null) {
      vbox.managedProperty().bind(visibility);
      vbox.visibleProperty().bind(visibility);
    }

    return vbox;
  }

  public static StylableVBox vbox(String styleClass, Node... nodes) {
    return vbox(styleClass, null, nodes);
  }

  public static StylableVBox vbox(BooleanBinding visibility, Node... nodes) {
    return vbox(null, visibility, nodes);
  }

  public static StylableVBox vbox(Node... nodes) {
    return vbox(null, null, nodes);
  }

  public static StylableStackPane stack(Node... nodes) {
    return stack(null, nodes);
  }

  public static StylableStackPane stack(String styleClass, Node... nodes) {
    return addStyleClass(new StylableStackPane(nodes), styleClass);
  }

  private static <T extends Node> T addStyleClass(T node, String styleClass) {
    if(styleClass != null) {
      node.getStyleClass().addAll(styleClass.split(",(?: *)"));
    }

    return node;
  }
}
