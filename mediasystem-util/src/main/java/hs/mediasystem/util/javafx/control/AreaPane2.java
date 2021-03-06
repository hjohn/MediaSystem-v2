package hs.mediasystem.util.javafx.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class AreaPane2<T> extends StackPane {
  private final Map<T, AreaDescriptor<? extends Parent>> areaDescriptors = new HashMap<>();
  private final Map<T, List<Node>> addedNodes = new HashMap<>();

  public boolean add(T area, Node node) {
    @SuppressWarnings("unchecked")
    AreaDescriptor<Parent> areaDescriptor = (AreaDescriptor<Parent>)areaDescriptors.get(area);

    if(areaDescriptor == null) {
      return false;
    }

    areaDescriptor.adder.accept(areaDescriptor.pane, node);

    addedNodes.computeIfAbsent(area, k -> new ArrayList<>()).add(node);

    node.parentProperty().addListener(new InvalidationListener() {
      @Override
      public void invalidated(Observable obs) {
        addedNodes.get(area).remove(node);
        node.parentProperty().removeListener(this);
      }
    });

    return true;
  }

  public boolean hasArea(T area) {
    return areaDescriptors.containsKey(area);
  }

  public void clear() {
    for(T area : addedNodes.keySet()) {
      clear(area);
    }

    addedNodes.clear();
  }

  public void clear(T area) {
    List<Node> nodes = new ArrayList<>(addedNodes.getOrDefault(area, Collections.emptyList()));

    for(Node node : nodes) {
      ((Pane)node.getParent()).getChildren().remove(node);
    }
  }

  protected <P extends Parent> void setupArea(T area, P pane, BiConsumer<P, Node> adder) {
    areaDescriptors.put(area, new AreaDescriptor<>(pane, adder));
  }

  protected void setupArea(T area, Pane pane) {
    areaDescriptors.put(area, new AreaDescriptor<>(pane, (p, n) -> p.getChildren().add(n)));
  }

  private static class AreaDescriptor<P extends Parent> {
    final P pane;
    final BiConsumer<P, Node> adder;

    public AreaDescriptor(P pane, BiConsumer<P, Node> adder) {
      this.pane = pane;
      this.adder = adder;
    }
  }
}
