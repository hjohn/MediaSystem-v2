package hs.mediasystem.util.javafx.control;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Provides convience methods for creating standard JavaFX node containers.<p>
 *
 * All the methods that accept a list of {@link Node}s will ignore <code>null</code>s
 * for convenience when programmaticaly constructing elements.<p>
 *
 * Multiple style classes can be assigned by providing a comma separated string.
 */
public class Containers {
  public static final Option HIDE_IF_EMPTY = pane -> hide(Bindings.isEmpty(pane.getChildren().filtered(Node::isVisible)));
  public static final Option MOUSE_TRANSPARENT = pane -> pane.setMouseTransparent(true);

  public static final Option hideIfEmpty(StringExpression x) {
    return hide(x.isEmpty());
  }

  public static final Option hide(BooleanExpression x) {
    return pane -> {
      pane.managedProperty().bind(x.not());
      pane.visibleProperty().bind(x.not());
    };
  }

  public static final Option apply(Consumer<Pane> paneConsumer) {
    return pane -> paneConsumer.accept(pane);
  }

  public interface Option extends Consumer<Pane> {
  }

  public static HBox hbox(String styleClass, List<Node> nodes, Option... options) {
    HBox hbox = new HBox();

    addChildren(hbox, nodes);
    addStyleClass(hbox, styleClass);

    for(Option option : options) {
      option.accept(hbox);
    }

    return hbox;
  }

  public static HBox hbox(String styleClass, Node... nodes) {
    return hbox(styleClass, Arrays.asList(nodes));
  }

  public static HBox hbox(Node... nodes) {
    return hbox(null, nodes);
  }

  public static VBox vbox(String styleClass, BooleanBinding visibility, List<Node> nodes, Option... options) {
    VBox vbox = new VBox();

    addChildren(vbox, nodes);
    addStyleClass(vbox, styleClass);

    if(visibility != null) {
      vbox.managedProperty().bind(visibility);
      vbox.visibleProperty().bind(visibility);
    }

    for(Option option : options) {
      option.accept(vbox);
    }

    return vbox;
  }

  public static VBox vbox(String styleClass, BooleanBinding visibility, Node... nodes) {
    return vbox(styleClass, visibility, Arrays.asList(nodes));
  }

  public static VBox vbox(String styleClass, List<Node> nodes, Option... options) {
    return vbox(styleClass, null, nodes, options);
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
    return stack(styleClass, Arrays.asList(nodes));
  }

  public static StackPane stack(String styleClass, List<Node> nodes, Option... options) {
    StackPane stackPane = new StackPane();

    addChildren(stackPane, nodes);

    for(Option option : options) {
      option.accept(stackPane);
    }

    return addStyleClass(stackPane, styleClass);
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

  private static void addChildren(Pane pane, List<Node> nodes) {
    nodes.stream().filter(Objects::nonNull).forEach(n -> pane.getChildren().add(n));
  }
}
