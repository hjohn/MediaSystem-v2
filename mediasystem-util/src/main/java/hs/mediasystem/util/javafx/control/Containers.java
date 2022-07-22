package hs.mediasystem.util.javafx.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Provides convenience methods for creating standard JavaFX node containers.<p>
 *
 * All the methods that accept a list of {@link Node}s will ignore <code>null</code>s
 * for convenience when programmatically constructing elements.<p>
 *
 * Multiple style classes can be assigned by providing a comma separated string.
 */
public class Containers {
  public static final Consumer<Pane> IGNORE_IF_EMPTY = pane -> {
    BooleanBinding hasNoManagedChildren = new BooleanBinding() {
      InvalidationListener managedListener = obs -> invalidate();

      {
        pane.getChildren().forEach(n -> n.managedProperty().addListener(managedListener));
        pane.getChildren().addListener((ListChangeListener<Node>)c -> {
          boolean invalidate = false;

          while(c.next()) {
            if(c.wasAdded() || c.wasRemoved()) {
              c.getAddedSubList().forEach(n -> n.managedProperty().addListener(managedListener));
              c.getRemoved().forEach(n -> n.managedProperty().removeListener(managedListener));
              invalidate = true;
            }
          }

          if(invalidate) {
            invalidate();
          }
        });
      }

      @Override
      protected boolean computeValue() {
        return !pane.getChildren().stream().anyMatch(Node::isManaged);
      }
    };

    bindIgnore(pane, hasNoManagedChildren);
  };

  public static HBox hbox(String styleClass, Node... nodes) {
    return hbox().style(styleClass.split(",(?: *)")).nodes(nodes);
  }

  public static HBox hbox(Node... nodes) {
    return hbox().nodes(nodes);
  }

  public static VBox vbox(String styleClass, Node... nodes) {
    return vbox().style(styleClass.split(",(?: *)")).nodes(nodes);
  }

  public static VBox vbox(Node... nodes) {
    return vbox().nodes(nodes);
  }

  public static GridPane grid(String styleClass) {
    return addStyleClass(new GridPane(), styleClass);
  }

  public static StackPane stack(Node... nodes) {
    return stack().nodes(nodes);
  }

  public static StackPane stack(String styleClass, Node... nodes) {
    return stack().style(styleClass.split(",(?: *)")).nodes(nodes);
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

  public static ContainerBuilder<HBox> hbox() {
    return new ContainerBuilder<>(HBox::new);
  }

  public static ContainerBuilder<VBox> vbox() {
    return new ContainerBuilder<>(VBox::new);
  }

  public static ContainerBuilder<StackPane> stack() {
    return new ContainerBuilder<>(StackPane::new);
  }

  private static void bindIgnore(Pane pane, ObservableValue<Boolean> x) {
    pane.managedProperty().bind(x.map(b -> !b).orElse(false));
    pane.visibleProperty().bind(x.map(b -> !b).orElse(false));
  }

  public static class ContainerBuilder<T extends Pane> {
    private final List<Consumer<? super T>> options = new ArrayList<>();
    private final List<String> styleClasses = new ArrayList<>();
    private final Supplier<T> nodeSupplier;

    public ContainerBuilder(Supplier<T> nodeSupplier) {
      this.nodeSupplier = nodeSupplier;
    }

    public ContainerBuilder<T> style(String... styleClasses) {
      this.styleClasses.addAll(Arrays.asList(styleClasses));
      return this;
    }

    public ContainerBuilder<T> ignoreWhenEmpty() {
      options.add(Containers.IGNORE_IF_EMPTY);
      return this;
    }

    public ContainerBuilder<T> ignoreWhen(ObservableValue<Boolean> expression) {
      options.add(pane -> bindIgnore(pane, expression));
      return this;
    }

    public ContainerBuilder<T> mouseTransparent() {
      options.add(pane -> pane.setMouseTransparent(true));
      return this;
    }

    public T nodes(List<Node> nodes) {
      T container = nodeSupplier.get();

      addChildren(container, nodes);

      container.getStyleClass().addAll(styleClasses);

      for(Consumer<? super T> option : options) {
        option.accept(container);
      }

      return container;
    }

    public T nodes(Node... nodes) {
      return nodes(Arrays.asList(nodes));
    }
  }
}
