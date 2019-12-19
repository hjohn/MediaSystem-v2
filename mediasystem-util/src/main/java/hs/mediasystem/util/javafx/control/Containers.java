package hs.mediasystem.util.javafx.control;

import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class Containers {

  public static HBox hbox(String styleClass, Node... nodes) {
    HBox hbox = new HBox(nodes);

    addStyleClass(hbox, styleClass);

    return hbox;
  }

  public static HBox hbox(Node... nodes) {
    return hbox(null, nodes);
  }

  public static VBox vbox(String styleClass, BooleanBinding visibility, Node... nodes) {
    VBox vbox = new VBox(nodes);

    addStyleClass(vbox, styleClass);

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

  public static GridPane grid(String styleClass) {
    return addStyleClass(new GridPane(), styleClass);
  }

  public static StackPane stack(Node... nodes) {
    return stack(null, nodes);
  }

  public static StackPane stack(String styleClass, Node... nodes) {
    return addStyleClass(new StackPane(nodes), styleClass);
  }

  public static BorderPane border(String styleClass, Node center, Node left, Node right, Node top, Node bottom) {
    BorderPane borderPane = new BorderPane();

    borderPane.setCenter(center);
    borderPane.setLeft(left);
    borderPane.setRight(right);
    borderPane.setTop(top);
    borderPane.setBottom(bottom);

    return addStyleClass(borderPane, styleClass);
  }

  private static <T extends Node> T addStyleClass(T node, String styleClass) {
    if(styleClass != null) {
      node.getStyleClass().addAll(styleClass.split(",(?: *)"));
    }

    return node;
  }
}
