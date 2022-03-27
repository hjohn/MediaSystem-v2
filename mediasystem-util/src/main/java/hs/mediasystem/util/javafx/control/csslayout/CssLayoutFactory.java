package hs.mediasystem.util.javafx.control.csslayout;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Creates new nodes for CSS based layout.<p>
 *
 * Accepted format: <code>[(container-type!)style-name: ({key: value (, ...) })(, ...)]</code><p>
 *
 * Where:<ul>
 *   <li>{@code container-type} is optionally one of {@code HBox, VBox, Stack}</li>
 *   <li>{@code style-name} is a Style class of a child control to place at the position it appears</li>
 *   <li>{@code key} and {@code value} are key value pairs with extra options.
 *   </ul>
 *
 * Supported options are:<ul>
 *
 *   <li>{@code align} for aligning a child in a {@link StackPane}, see {@link Pos} for allowed values</li>
 *   <li>{@code hgrow} and {@code vgrow}, see {@link Priority} for allowed values</li>
 *   </ul>
 *
 * Examples:<ul>
 * <li><code>[title, VBox!subtitle-block: {hgrow: ALWAYS}, VBox!side-box]</code></li>
 * </ul>
 */
public class CssLayoutFactory {
  private static final String POTENTIALS = "css-layout-factory.potential-children";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

  private static ChildDefinition toChildDefinition(Object obj) {
    if(obj instanceof String) {
      return new ChildDefinition((String)obj, Map.of());
    }

    if(obj instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>)obj;

      if(map.size() != 1) {
        throw new IllegalStateException("Expected map of exactly 1 element: " + map);
      }

      Map.Entry<String, Object> entry = map.entrySet().iterator().next();

      @SuppressWarnings("unchecked")
      Map<String, Object> value = (Map<String, Object>)entry.getValue();

      return new ChildDefinition(entry.getKey(), value);
    }

    throw new IllegalStateException("Did not expect type: " + obj);
  }

  private static List<ChildDefinition> fromYaml(String v) {
    try {
      List<Object> list = OBJECT_MAPPER.readValue(v, new TypeReference<List<Object>>() {});

      return list.stream()
        .map(CssLayoutFactory::toChildDefinition)
        .collect(Collectors.toList());
    }
    catch(IOException e) {
      throw new IllegalStateException("Problem parsing \"" + v + "\"", e);
    }
  }

  public static void setPotentials(Pane parent, List<Node> potentials) {
    parent.getProperties().put(POTENTIALS, potentials);
  }

  public static <T extends Pane & Resolvable> void resolveChildren(T pane) {
    if(!pane.isResolved()) {
      String layout = pane.getNodeLayout();

      if(layout != null && !layout.isEmpty()) {
        pane.getChildren().setAll(createNewChildren(layout, pane));
        pane.setResolved();
      }
    }
  }

  private static List<Node> createNewChildren(String v, Pane parent) {
    List<Node> newChildren = new ArrayList<>();
    List<Node> children = new ArrayList<>(parent.getChildren());
    List<ChildDefinition> childDefinitions = fromYaml(v);

    @SuppressWarnings("unchecked")
    List<Node> potentialChildren = (List<Node>)parent.getProperties().getOrDefault(POTENTIALS, List.of());

    children.addAll(potentialChildren);

    for(ChildDefinition childDef : childDefinitions) {
      Node node;

      if(childDef.getType() == null) {
        node = children.stream().filter(c -> c.getStyleClass().stream().anyMatch(sc -> sc.equals(childDef.getName()))).findFirst()
          .orElseGet(() -> Labels.create("", "Child '" + childDef.getName() + "' not found"));
      }
      else {
        if(childDef.getType().equals("HBox")) {
          node = new StylableHBox();

          Containers.IGNORE_IF_EMPTY.accept((Pane)node);
        }
        else if(childDef.getType().equals("VBox")) {
          node = new StylableVBox();

          Containers.IGNORE_IF_EMPTY.accept((Pane)node);
        }
        else if(childDef.getType().equals("Stack")) {
          node = new StylableStackPane();

          Containers.IGNORE_IF_EMPTY.accept((Pane)node);
        }
        else {
          node = Labels.create("", "Unknown container type: " + childDef.getType());
        }

        node.getStyleClass().add(childDef.getName());
        node.getProperties().put(POTENTIALS, children);
      }

      String hgrow = childDef.getString("hgrow");

      if(hgrow != null) {
        HBox.setHgrow(node, Priority.valueOf(hgrow.toUpperCase()));
      }

      String vgrow = childDef.getString("vgrow");

      if(vgrow != null) {
        VBox.setVgrow(node, Priority.valueOf(vgrow.toUpperCase()));
      }

      String align = childDef.getString("align");

      if(align != null) {
        StackPane.setAlignment(node, Pos.valueOf(align.toUpperCase()));
      }

      childDef.getList("margins", Double.class, 4).ifPresent(list -> {
        Insets insets = new Insets(list.get(0), list.get(1), list.get(2), list.get(3));
        StylableStackPane.setPercentageMargin(node, insets);
      });

      newChildren.add(node);
    }

    return newChildren;
  }

  private static class ChildDefinition {
    private final String type;
    private final String name;
    private final Map<String, Object> options;

    public ChildDefinition(String type, String name, Map<String, Object> options) {
      this.type = type;
      this.name = name;
      this.options = options;
    }

    public ChildDefinition(String name, Map<String, Object> options) {
      this(extractType(name), extractName(name), options);
    }

    private static String extractName(String name) {
      int mark = name.indexOf("!");

      return mark >= 0 ? name.substring(mark + 1) : name;
    }

    private static String extractType(String name) {
      int mark = name.indexOf("!");

      return mark >= 0 ? name.substring(0, mark) : null;
    }

    public String getType() {
      return type;
    }

    public String getName() {
      return name;
    }

    public String getString(String optionName) {
      return (String)options.get(optionName);
    }

    public <T> Optional<List<T>> getList(String optionName, Class<T> cls, int count) {
      Object obj = options.get(optionName);

      if(obj == null) {
        return Optional.empty();
      }

      if(obj instanceof List) {
        @SuppressWarnings("unchecked")
        List<T> list = (List<T>)obj;

        if(list.size() == count) {
          if(list.stream().allMatch(e -> cls.isInstance(e))) {
            return Optional.of(list);
          }
        }
      }

      throw new IllegalArgumentException("Expected list of type " + cls + " with " + count + " elements: " + obj);
    }
  }
}
